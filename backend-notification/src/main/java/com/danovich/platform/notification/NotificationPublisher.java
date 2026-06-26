package com.danovich.platform.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

/**
 * The publish side of the notification contract. A host app hands it a
 * {@link NotificationRequest}; the publisher applies the app's {@link NotificationGate}
 * (is this recipient enabled, and where to?) and, if enabled, writes a self-contained
 * {@code {to, text}} record onto a durable Redis stream. The gate runs here — at
 * publish — so what lands on the stream is already addressed and consent-checked, and
 * the consumer can be a wholly separate service that knows nothing about the app.
 *
 * <p>Disabled (or no destination) means nothing is published — the request is ignored.
 */
public class NotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationPublisher.class);

    private final NotificationGate gate;
    private final StringRedisTemplate redis;
    private final String streamKey;

    public NotificationPublisher(NotificationGate gate, StringRedisTemplate redis, String streamKey) {
        this.gate = gate;
        this.redis = redis;
        this.streamKey = streamKey;
    }

    /** Resolve+gate the recipient and enqueue the message, or ignore it if disabled. */
    public void publish(NotificationRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            return;
        }
        gate.destinationIfEnabled(request.recipientRef()).ifPresentOrElse(
                to -> {
                    redis.opsForStream().add(StreamRecords.newRecord()
                            .in(streamKey)
                            .ofMap(Map.of("to", to, "text", request.message())));
                    log.debug("Queued notification for {} on stream {}", to, streamKey);
                },
                () -> log.debug("Notification ignored for recipientRef={} (disabled or no destination)",
                        request.recipientRef()));
    }

    /** Convenience for {@code publish(new NotificationRequest(recipientRef, message))}. */
    public void notify(String recipientRef, String message) {
        publish(new NotificationRequest(recipientRef, message));
    }
}
