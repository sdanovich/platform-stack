package com.danovich.platform.notification.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Everything a project configures to use the platform notification module. All
 * values come from the project's environment / application config — nothing is
 * baked into the library, and secrets never live here.
 *
 * <p>Example {@code application.yml}:
 * <pre>
 * platform:
 *   notification:
 *     sms:
 *       account-sid: ${TWILIO_ACCOUNT_SID:}
 *       auth-token:  ${TWILIO_AUTH_TOKEN:}
 *       from:        ${TWILIO_FROM:}
 * </pre>
 *
 * Leaving the SMS credentials blank disables outbound SMS gracefully.
 */
@ConfigurationProperties(prefix = "platform.notification")
public class NotificationProperties {

    private final Sms sms = new Sms();

    public Sms getSms() {
        return sms;
    }

    /** SMS-over-Twilio settings. */
    public static class Sms {

        /** Twilio Account SID. */
        private String accountSid = "";

        /** Twilio Auth Token. */
        private String authToken = "";

        /** The Twilio phone number messages are sent from (E.164, e.g. +15551234567). */
        private String from = "";

        public String getAccountSid() { return accountSid; }
        public void setAccountSid(String v) { this.accountSid = v; }

        public String getAuthToken() { return authToken; }
        public void setAuthToken(String v) { this.authToken = v; }

        public String getFrom() { return from; }
        public void setFrom(String v) { this.from = v; }
    }
}
