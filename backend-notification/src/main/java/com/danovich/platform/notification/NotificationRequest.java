package com.danovich.platform.notification;

/**
 * A request to notify a recipient — the message a host app publishes when it
 * decides something is worth telling someone about. The module is agnostic about
 * <em>why</em>: it neither knows nor cares what produced the message.
 *
 * <p>Publish it as a Spring application event and the module's
 * {@link NotificationDispatcher} consumes it, or hand it to the dispatcher
 * directly:
 * <pre>
 *   eventPublisher.publishEvent(new NotificationRequest(userId, "…"));
 * </pre>
 *
 * @param recipientRef opaque key identifying the recipient; the app's
 *                     {@link NotificationGate} resolves it to a destination and an
 *                     enabled/disabled decision (e.g. a user id → their phone number)
 * @param message      the text to deliver
 */
public record NotificationRequest(String recipientRef, String message) {
}
