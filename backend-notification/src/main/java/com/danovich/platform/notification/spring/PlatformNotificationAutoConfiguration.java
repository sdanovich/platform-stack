package com.danovich.platform.notification.spring;

import com.danovich.platform.notification.SmsNotifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Drop-in wiring for the platform notification module. Registered as a Spring Boot
 * auto-configuration (see {@code META-INF/spring/...AutoConfiguration.imports}), so
 * a {@code @SpringBootApplication} gets the {@link SmsNotifier} bean automatically
 * from a few properties under {@code platform.notification.*}. No {@code @Import}
 * is required.
 *
 * <p>An app that wants different behavior can declare its own {@code SmsNotifier}
 * bean — {@link ConditionalOnMissingBean} backs off in that case.
 */
@Configuration
@EnableConfigurationProperties(NotificationProperties.class)
public class PlatformNotificationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SmsNotifier smsNotifier(NotificationProperties props) {
        NotificationProperties.Sms sms = props.getSms();
        return new SmsNotifier(sms.getAccountSid(), sms.getAuthToken(), sms.getFrom());
    }
}
