package com.danovich.platform.login;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import java.util.Collections;

/**
 * Verifies Google ID tokens (signature, expiry, and audience = our Web client id),
 * returning the verified email. Dormant when no client id is configured. Wired as
 * a bean by {@code LoginAutoConfiguration} from {@code oauth.google.client-id}.
 */
public class GoogleVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleVerifier(String clientId) {
        this.verifier = (clientId == null || clientId.isBlank()) ? null
                : new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                        .setAudience(Collections.singletonList(clientId))
                        .build();
    }

    public boolean configured() {
        return verifier != null;
    }

    /** @return the verified email, or null if the token is invalid/unverified. */
    public String verifiedEmail(String idToken) {
        if (verifier == null || idToken == null || idToken.isBlank()) {
            return null;
        }
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                return null;
            }
            GoogleIdToken.Payload p = token.getPayload();
            if (!Boolean.TRUE.equals(p.getEmailVerified())) {
                return null;
            }
            return p.getEmail();
        } catch (Exception e) {
            return null;
        }
    }
}
