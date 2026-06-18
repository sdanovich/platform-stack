package com.danovich.platform.login;

import com.danovich.platform.login.AuthDtos.GitHubRequest;
import com.danovich.platform.login.AuthDtos.GoogleRequest;
import com.danovich.platform.login.domain.User;
import com.danovich.platform.login.repo.UserRepo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;

/**
 * Google/GitHub sign-in. Each verifies the provider credential server-side, then
 * finds-or-creates a user by verified email (linking key) and returns the same
 * JWT + refresh token as email login (via {@link AuthController#issue}). Endpoints
 * return 503 when the provider isn't configured. New accounts fire
 * {@link UserCreatedCallback}.
 */
@RestController
@RequestMapping("/api/auth")
public class SocialAuthController {

    private final GoogleVerifier google;
    private final GitHubClient github;
    private final UserRepo userRepo;
    private final UserCreatedCallback onUserCreated;
    private final AuthController auth;

    public SocialAuthController(GoogleVerifier google, GitHubClient github, UserRepo userRepo,
                                UserCreatedCallback onUserCreated, AuthController auth) {
        this.google = google;
        this.github = github;
        this.userRepo = userRepo;
        this.onUserCreated = onUserCreated;
        this.auth = auth;
    }

    @PostMapping("/google")
    public ResponseEntity<?> google(@RequestBody GoogleRequest req) {
        if (!google.configured()) {
            return disabled("Google sign-in is not configured");
        }
        String email = google.verifiedEmail(req.idToken());
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid Google token"));
        }
        return ResponseEntity.ok(auth.issue(findOrCreate(email, "GOOGLE")));
    }

    @PostMapping("/github")
    public ResponseEntity<?> github(@RequestBody GitHubRequest req) {
        if (!github.configured()) {
            return disabled("GitHub sign-in is not configured");
        }
        String email = github.emailForCode(req.code());
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "GitHub sign-in failed"));
        }
        return ResponseEntity.ok(auth.issue(findOrCreate(email, "GITHUB")));
    }

    private User findOrCreate(String rawEmail, String provider) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        return userRepo.findByEmail(email).orElseGet(() -> {
            User u = userRepo.save(new User(email, provider, email));
            onUserCreated.onUserCreated(u.getId(), u.getEmail(), provider);
            return u;
        });
    }

    private ResponseEntity<?> disabled(String msg) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", msg));
    }
}
