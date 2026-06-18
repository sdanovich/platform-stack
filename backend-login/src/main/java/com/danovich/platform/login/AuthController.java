package com.danovich.platform.login;

import com.danovich.platform.login.AuthDtos.AuthRequest;
import com.danovich.platform.login.AuthDtos.AuthResponse;
import com.danovich.platform.login.AuthDtos.RefreshRequest;
import com.danovich.platform.login.domain.User;
import com.danovich.platform.login.repo.UserRepo;
import com.danovich.platform.auth.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;

/**
 * Email/password accounts + token refresh. Passwords are bcrypt-hashed; on success
 * we mint a short-lived JWT (subject = user id, via the platform {@link JwtService})
 * plus a long-lived refresh token. Social sign-in lives in {@link SocialAuthController}
 * and shares {@link #issue(User)}. New accounts fire {@link UserCreatedCallback} so a
 * consuming app can run post-signup setup.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepo userRepo;
    private final BCryptPasswordEncoder encoder;
    private final JwtService jwt;
    private final RefreshService refreshService;
    private final UserCreatedCallback onUserCreated;

    public AuthController(UserRepo userRepo, BCryptPasswordEncoder encoder, JwtService jwt,
                          RefreshService refreshService, UserCreatedCallback onUserCreated) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwt = jwt;
        this.refreshService = refreshService;
        this.onUserCreated = onUserCreated;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest req) {
        String email = normalize(req.email());
        String password = req.password() == null ? "" : req.password();
        if (email.isBlank() || !email.contains("@") || password.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "valid email and password (min 6 chars) required"));
        }
        if (userRepo.existsByEmail(email)) {
            return ResponseEntity.status(409).body(Map.of("error", "email already registered"));
        }
        User u = userRepo.save(new User(email, encoder.encode(password)));
        onUserCreated.onUserCreated(u.getId(), u.getEmail(), "LOCAL");
        return ResponseEntity.ok(issue(u));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        String email = normalize(req.email());
        String password = req.password() == null ? "" : req.password();
        User u = userRepo.findByEmail(email).orElse(null);
        if (u == null || u.getPasswordHash() == null
                || !encoder.matches(password, u.getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid email or password"));
        }
        return ResponseEntity.ok(issue(u));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest req) {
        return refreshService.rotate(req.refreshToken())
                .flatMap(r -> userRepo.findById(r.userId())
                        .map(u -> ResponseEntity.ok((Object) new AuthResponse(
                                jwt.issue(u.getId().toString()), jwt.getTtlSeconds(),
                                u.getId().toString(), u.getEmail(), r.refreshToken()))))
                .orElseGet(() -> ResponseEntity.status(401)
                        .body(Map.of("error", "invalid or expired refresh token")));
    }

    /** Build a JWT + a fresh refresh token for a user. Shared with social sign-in. */
    public AuthResponse issue(User u) {
        String refresh = refreshService.issue(u.getId());
        return new AuthResponse(jwt.issue(u.getId().toString()), jwt.getTtlSeconds(),
                u.getId().toString(), u.getEmail(), refresh);
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
