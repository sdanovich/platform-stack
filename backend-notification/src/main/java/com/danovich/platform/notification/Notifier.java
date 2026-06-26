package com.danovich.platform.notification;

/**
 * A transport that physically delivers a message to a recipient (SMS, etc.).
 * Implementations degrade gracefully (log + no-op) when unconfigured, so a host
 * app without credentials still runs. The notification contract is transport-
 * agnostic: swap the {@link Notifier} bean to change how messages go out.
 *
 * @see SmsNotifier Twilio implementation
 * @see TextBeeNotifier TextBee (Android-gateway) implementation
 */
public interface Notifier {
    void send(String to, String text);
}
