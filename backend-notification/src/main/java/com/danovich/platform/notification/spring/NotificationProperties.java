package com.danovich.platform.notification.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Everything a project configures to use the platform notification module. All
 * values come from the project's environment / application config — nothing is
 * baked into the library, and secrets never live here.
 *
 * <p>{@code provider} picks the transport ({@code twilio} or {@code textbee}); only
 * that provider's settings need filling in. Leaving the chosen provider's
 * credentials blank disables outbound SMS gracefully.
 *
 * <p>Example {@code application.yml} (Twilio):
 * <pre>
 * platform:
 *   notification:
 *     sms:
 *       provider:    twilio
 *       account-sid: ${TWILIO_ACCOUNT_SID:}
 *       auth-token:  ${TWILIO_AUTH_TOKEN:}
 *       from:        ${TWILIO_FROM:}
 * </pre>
 *
 * Example (TextBee — Android phone as gateway):
 * <pre>
 * platform:
 *   notification:
 *     sms:
 *       provider:  textbee
 *       textbee:
 *         api-key:   ${TEXTBEE_API_KEY:}
 *         device-id: ${TEXTBEE_DEVICE_ID:}
 * </pre>
 */
@ConfigurationProperties(prefix = "platform.notification")
public class NotificationProperties {

    private final Sms sms = new Sms();
    private final Stream stream = new Stream();

    public Sms getSms() {
        return sms;
    }

    public Stream getStream() {
        return stream;
    }

    /** Redis-stream exchange the publisher writes to and the consumer reads from. */
    public static class Stream {

        /** Stream key notifications are XADDed to. */
        private String key = "platform:notifications";

        /** Consumer-group name (one group across all instances of an app). */
        private String group = "notifier";

        /** This instance's consumer name within the group. Blank = derive a default. */
        private String consumer = "";

        public String getKey() { return key; }
        public void setKey(String v) { this.key = v; }

        public String getGroup() { return group; }
        public void setGroup(String v) { this.group = v; }

        public String getConsumer() { return consumer; }
        public void setConsumer(String v) { this.consumer = v; }
    }

    /** SMS settings. {@code provider} selects which transport's fields are used. */
    public static class Sms {

        /** Transport to use: {@code twilio} (default) or {@code textbee}. */
        private String provider = "twilio";

        // --- Twilio ---

        /** Twilio Account SID. */
        private String accountSid = "";

        /** Twilio Auth Token. */
        private String authToken = "";

        /** The Twilio phone number messages are sent from (E.164, e.g. +15551234567). */
        private String from = "";

        // --- TextBee ---

        private final TextBee textbee = new TextBee();

        public String getProvider() { return provider; }
        public void setProvider(String v) { this.provider = v; }

        public String getAccountSid() { return accountSid; }
        public void setAccountSid(String v) { this.accountSid = v; }

        public String getAuthToken() { return authToken; }
        public void setAuthToken(String v) { this.authToken = v; }

        public String getFrom() { return from; }
        public void setFrom(String v) { this.from = v; }

        public TextBee getTextbee() { return textbee; }

        /** TextBee Android-gateway settings. */
        public static class TextBee {

            /** TextBee API key (x-api-key). */
            private String apiKey = "";

            /** TextBee device id that relays the SMS. */
            private String deviceId = "";

            /** TextBee API base URL; the public default suits most setups. */
            private String baseUrl = "https://api.textbee.dev/api/v1";

            public String getApiKey() { return apiKey; }
            public void setApiKey(String v) { this.apiKey = v; }

            public String getDeviceId() { return deviceId; }
            public void setDeviceId(String v) { this.deviceId = v; }

            public String getBaseUrl() { return baseUrl; }
            public void setBaseUrl(String v) { this.baseUrl = v; }
        }
    }
}
