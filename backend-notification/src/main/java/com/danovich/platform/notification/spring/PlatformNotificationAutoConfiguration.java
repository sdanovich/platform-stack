package com.danovich.platform.notification.spring;

import com.danovich.platform.notification.AlwaysEnabledGate;
import com.danovich.platform.notification.NotificationGate;
import com.danovich.platform.notification.NotificationPublisher;
import com.danovich.platform.notification.NotificationStreamConsumer;
import com.danovich.platform.notification.Notifier;
import com.danovich.platform.notification.SmsNotifier;
import com.danovich.platform.notification.TextBeeNotifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Drop-in wiring for the platform notification module. Registered as a Spring Boot
 * auto-configuration (see {@code META-INF/spring/...AutoConfiguration.imports}), it
 * gives a {@code @SpringBootApplication}:
 *
 * <ul>
 *   <li>a {@link Notifier} transport chosen by {@code platform.notification.sms.provider}
 *       ({@code twilio} or {@code textbee}) — override by declaring your own bean;</li>
 *   <li>a default {@link NotificationGate} (the app normally declares its own to gate on
 *       a per-user preference);</li>
 *   <li>a {@link NotificationPublisher} and a durable {@link NotificationStreamConsumer}
 *       over Redis Streams — active only when a {@link RedisConnectionFactory} is present.</li>
 * </ul>
 *
 * The publisher gates and enqueues; the consumer delivers. Both halves talk only through
 * the stream, so the consumer can later be split into its own service unchanged.
 */
@AutoConfiguration(after = RedisAutoConfiguration.class)
@EnableConfigurationProperties(NotificationProperties.class)
public class PlatformNotificationAutoConfiguration {

    /** The selected SMS transport. An app can replace it by declaring its own {@link Notifier}. */
    @Bean
    @ConditionalOnMissingBean(Notifier.class)
    public Notifier notifier(NotificationProperties props) {
        NotificationProperties.Sms sms = props.getSms();
        if ("textbee".equalsIgnoreCase(sms.getProvider())) {
            return new TextBeeNotifier(
                    sms.getTextbee().getApiKey(),
                    sms.getTextbee().getDeviceId(),
                    sms.getTextbee().getBaseUrl());
        }
        return new SmsNotifier(sms.getAccountSid(), sms.getAuthToken(), sms.getFrom());
    }

    /** Default gate: recipientRef is the destination, always enabled. Apps usually override. */
    @Bean
    @ConditionalOnMissingBean(NotificationGate.class)
    public NotificationGate notificationGate() {
        return new AlwaysEnabledGate();
    }

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean
    public NotificationPublisher notificationPublisher(NotificationGate gate,
                                                       StringRedisTemplate redis,
                                                       NotificationProperties props) {
        return new NotificationPublisher(gate, redis, props.getStream().getKey());
    }

    @Bean
    @ConditionalOnBean({RedisConnectionFactory.class, StringRedisTemplate.class})
    @ConditionalOnMissingBean
    public NotificationStreamConsumer notificationStreamConsumer(Notifier notifier,
                                                                 RedisConnectionFactory connectionFactory,
                                                                 StringRedisTemplate redis,
                                                                 NotificationProperties props) {
        NotificationProperties.Stream s = props.getStream();
        String consumer = s.getConsumer();
        if (consumer == null || consumer.isBlank()) {
            // A per-instance name; uniqueness only matters for pending-entry ownership.
            consumer = "consumer-" + Long.toHexString(ProcessHandle.current().pid());
        }
        return new NotificationStreamConsumer(
                notifier, connectionFactory, redis, s.getKey(), s.getGroup(), consumer);
    }
}
