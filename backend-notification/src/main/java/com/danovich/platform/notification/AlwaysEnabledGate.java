package com.danovich.platform.notification;

import java.util.Optional;

/**
 * Default {@link NotificationGate} used when the host app declares none: treats the
 * {@code recipientRef} as the destination and is always enabled (empty/blank refs
 * are ignored). Apps that gate on a per-user preference declare their own bean,
 * which the auto-configuration backs off to.
 */
public class AlwaysEnabledGate implements NotificationGate {

    @Override
    public Optional<String> destinationIfEnabled(String recipientRef) {
        if (recipientRef == null || recipientRef.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(recipientRef);
    }
}
