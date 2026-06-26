package com.danovich.platform.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Sends outbound SMS through the TextBee Android-gateway API
 * ({@code POST {base}/gateway/devices/{deviceId}/send-sms}, authenticated with an
 * {@code x-api-key} header). TextBee relays the message over your own phone's SIM,
 * so there is no per-message charge and no carrier A2P registration.
 *
 * <p>One of the {@link Notifier} transports; select it with
 * {@code platform.notification.sms.provider=textbee}. If the api-key/device-id are
 * blank, sending is a logged no-op so the feature degrades gracefully. A send
 * failure is logged and swallowed — an SMS hiccup must never break the caller.
 *
 * <p>The JSON body is built by hand (no Jackson dependency) so the module stays
 * dependency-light; only the recipient and message are interpolated, both escaped.
 */
public class TextBeeNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(TextBeeNotifier.class);

    private final String apiKey;
    private final String deviceId;
    private final String baseUrl;
    private final boolean configured;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public TextBeeNotifier(String apiKey, String deviceId, String baseUrl) {
        this.apiKey = apiKey;
        this.deviceId = deviceId;
        this.baseUrl = (baseUrl == null || baseUrl.isBlank())
                ? "https://api.textbee.dev/api/v1" : baseUrl;
        this.configured = apiKey != null && !apiKey.isBlank() && deviceId != null && !deviceId.isBlank();
        if (!configured) {
            log.warn("TextBee not configured (api-key / device-id unset); outbound SMS disabled.");
        }
    }

    @Override
    public void send(String to, String text) {
        if (!configured) {
            log.info("SMS suppressed (TextBee unconfigured); would have texted {}", to);
            return;
        }
        if (to == null || to.isBlank()) {
            log.warn("SMS skipped: recipient is empty.");
            return;
        }
        try {
            String body = "{\"recipients\":[\"" + esc(to) + "\"],\"message\":\"" + esc(text) + "\"}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/gateway/devices/" + deviceId + "/send-sms"))
                    .timeout(Duration.ofSeconds(30))
                    .header("content-type", "application/json")
                    .header("x-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                log.error("TextBee send failed {}: {}", resp.statusCode(), resp.body());
            } else {
                log.info("SMS sent to {} via TextBee", to);
            }
        } catch (Exception e) {
            log.error("TextBee send error to {}: {}", to, e.getMessage());
        }
    }

    /** Minimal JSON string escaping for the two interpolated values. */
    private static String esc(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
