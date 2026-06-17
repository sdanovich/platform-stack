package com.danovich.platform.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * The token-exchange endpoint. A project that wants the default mapping
 * ({@code POST /api/auth/token}) can {@code @Import} the autoconfiguration, which
 * registers this controller. To use a different path, don't import it — write a
 * one-line controller in the project that delegates to {@link JwtService}.
 *
 * <p>Whatever path this is mounted at MUST be listed in the filter's public paths,
 * or the client could never obtain its first token.
 */
@RestController
public class TokenController {

    private final JwtService jwtService;
    private final String subject;

    public TokenController(JwtService jwtService, String subject) {
        this.jwtService = jwtService;
        this.subject = subject;
    }

    @PostMapping("/api/auth/token")
    public ResponseEntity<?> token(@RequestBody TokenRequest request) {
        if (request == null || !jwtService.verifyClientSecret(request.clientSecret())) {
            return ResponseEntity.status(401).body(new ErrorBody("invalid client secret"));
        }
        String jwt = jwtService.issue(subject);
        return ResponseEntity.ok(new TokenResponse(jwt, jwtService.getTtlSeconds()));
    }

    private record ErrorBody(String error) {
    }
}
