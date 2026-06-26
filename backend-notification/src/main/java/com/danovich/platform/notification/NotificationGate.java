package com.danovich.platform.notification;

import java.util.Optional;

/**
 * The host app's policy for a recipient: is notification enabled, and where does it
 * go? This is the one piece the module can't know — it's app-specific (a per-user
 * preference, a phone number, a quiet-hours rule, …). The app supplies a bean; the
 * {@link NotificationDispatcher} consults it for every {@link NotificationRequest}.
 *
 * <p>Returning a present destination means "send it there"; an empty result means
 * "ignore this" (disabled, no address, suppressed). The gate is the single answer to
 * "send SMS if it's enabled" — the module stays agnostic of the reason either way.
 *
 * <p>If the app declares no gate, the module falls back to a permissive default that
 * treats {@code recipientRef} itself as the destination and is always enabled.
 */
public interface NotificationGate {

    /** The destination to deliver to, or empty to ignore the request. */
    Optional<String> destinationIfEnabled(String recipientRef);
}
