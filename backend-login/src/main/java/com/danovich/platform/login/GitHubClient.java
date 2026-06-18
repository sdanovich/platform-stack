package com.danovich.platform.login;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * GitHub OAuth: exchanges an authorization code for an access token, then reads the
 * account's primary verified email. Token/secret are config-only; dormant when unset.
 * The base URLs are overridable so a test can point them at a mock server, and the
 * User-Agent is configurable so each consuming app identifies itself to GitHub.
 */
public class GitHubClient {

    private final String clientId;
    private final String clientSecret;
    private final String tokenUrl;
    private final String apiBase;
    private final String userAgent;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public GitHubClient(String clientId, String clientSecret, String tokenUrl,
                        String apiBase, String userAgent) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenUrl = tokenUrl;
        this.apiBase = apiBase;
        this.userAgent = (userAgent == null || userAgent.isBlank()) ? "platform-login" : userAgent;
    }

    public boolean configured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }

    /** Exchange an OAuth code for the account's primary verified email; null on failure. */
    public String emailForCode(String code) {
        if (!configured() || code == null || code.isBlank()) {
            return null;
        }
        try {
            String accessToken = exchange(code);
            if (accessToken == null) {
                return null;
            }
            return primaryVerifiedEmail(accessToken);
        } catch (Exception e) {
            return null;
        }
    }

    private String exchange(String code) throws Exception {
        String form = "client_id=" + enc(clientId)
                + "&client_secret=" + enc(clientSecret)
                + "&code=" + enc(code);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .timeout(Duration.ofSeconds(20))
                .header("accept", "application/json")
                .header("content-type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            return null;
        }
        JsonNode at = mapper.readTree(resp.body()).path("access_token");
        return at.isMissingNode() || at.isNull() ? null : at.asText();
    }

    private String primaryVerifiedEmail(String accessToken) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(apiBase + "/user/emails"))
                .timeout(Duration.ofSeconds(20))
                .header("accept", "application/vnd.github+json")
                .header("authorization", "Bearer " + accessToken)
                .header("user-agent", userAgent)
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            return null;
        }
        JsonNode arr = mapper.readTree(resp.body());
        String firstVerified = null;
        for (JsonNode e : arr) {
            if (e.path("verified").asBoolean(false)) {
                String email = e.path("email").asText(null);
                if (e.path("primary").asBoolean(false)) {
                    return email; // prefer the primary
                }
                if (firstVerified == null) {
                    firstVerified = email;
                }
            }
        }
        return firstVerified;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
