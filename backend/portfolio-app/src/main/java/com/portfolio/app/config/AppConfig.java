package com.portfolio.app.config;

import com.portfolio.app.security.RateLimitProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(RateLimitProperties.class)
public class AppConfig {

    /** Injected everywhere time matters, so tests can control it. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
