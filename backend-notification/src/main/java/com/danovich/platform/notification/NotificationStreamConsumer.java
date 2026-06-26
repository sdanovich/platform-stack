package com.danovich.platform.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;

import java.time.Duration;
import java.util.Map;

/**
 * The consume side of the notification contract: a durable Redis-stream consumer that
 * reads {@code {to, text}} records (written by {@link NotificationPublisher}) and hands
 * each to the configured {@link Notifier}. Uses a consumer group for at-least-once
 * delivery — messages survive consumer downtime and are redelivered if not acknowledged
 * — and is deliberately ignorant of why a message was sent. This is what lets the
 * notifier run as (or move to) a standalone service.
 */
public class NotificationStreamConsumer
        implements StreamListener<String, MapRecord<String, String, String>>, SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(NotificationStreamConsumer.class);

    private final Notifier notifier;
    private final RedisConnectionFactory connectionFactory;
    private final StringRedisTemplate redis;
    private final String streamKey;
    private final String group;
    private final String consumerName;

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private volatile boolean running;

    public NotificationStreamConsumer(Notifier notifier, RedisConnectionFactory connectionFactory,
                                      StringRedisTemplate redis, String streamKey,
                                      String group, String consumerName) {
        this.notifier = notifier;
        this.connectionFactory = connectionFactory;
        this.redis = redis;
        this.streamKey = streamKey;
        this.group = group;
        this.consumerName = consumerName;
    }

    @Override
    public void onMessage(MapRecord<String, String, String> record) {
        Map<String, String> body = record.getValue();
        String to = body.get("to");
        String text = body.get("text");
        try {
            notifier.send(to, text);
        } catch (Exception e) {
            log.error("Notifier failed for {} (record {}): {}", to, record.getId(), e.getMessage());
        } finally {
            // Ack regardless: the Notifier already swallows transport errors, so leaving
            // the message pending would only redeliver something that won't succeed.
            redis.opsForStream().acknowledge(group, record);
        }
    }

    @Override
    public void start() {
        ensureGroup();
        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(1))
                        .build();
        container = StreamMessageListenerContainer.create(connectionFactory, options);
        container.receive(
                Consumer.from(group, consumerName),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
                this);
        container.start();
        running = true;
        log.info("Notification stream consumer started: stream={} group={} consumer={}",
                streamKey, group, consumerName);
    }

    /** Create the consumer group (and the stream) if it doesn't already exist. */
    private void ensureGroup() {
        try {
            redis.opsForStream().createGroup(streamKey, ReadOffset.from("0"), group);
        } catch (Exception e) {
            // BUSYGROUP — group already exists — is the normal case after first boot.
            log.debug("Consumer group {} on {} already present or not created: {}",
                    group, streamKey, e.getMessage());
        }
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
