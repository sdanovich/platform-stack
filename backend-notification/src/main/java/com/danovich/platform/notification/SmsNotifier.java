package com.danovich.platform.notification;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends outbound SMS via Twilio. Framework-agnostic: it is constructed with plain
 * credentials (the consuming app's {@code PlatformNotificationAutoConfiguration}
 * builds it from {@code platform.notification.sms.*}). If credentials are absent,
 * the notifier stays disabled and {@link #send} logs and no-ops, so an app without
 * Twilio configured still runs.
 *
 * <p>One of the {@link Notifier} transports; select it with
 * {@code platform.notification.sms.provider=twilio} (the default).
 */
public class SmsNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(SmsNotifier.class);

    private final String from;
    private final boolean ready;

    public SmsNotifier(String accountSid, String authToken, String from) {
        this.from = from;
        if (accountSid != null && !accountSid.isBlank() && authToken != null && !authToken.isBlank()) {
            Twilio.init(accountSid, authToken);
            this.ready = true;
        } else {
            log.warn("Twilio not configured; SMS disabled.");
            this.ready = false;
        }
    }

    @Override
    public void send(String to, String text) {
        if (!ready || to == null || to.isBlank()) {
            log.info("SMS suppressed (not ready or no recipient): {}", text);
            return;
        }
        Message.creator(new PhoneNumber(to), new PhoneNumber(from), text).create();
        log.info("SMS sent to {}", to);
    }
}
