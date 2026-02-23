package com.shopscale.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Slf4j
@Configuration
public class RetryConfig {

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Retry policy: max 3 attempts
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);

        // Exponential backoff: 2s, 4s, 8s (max 10s)
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(2000);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        // Add retry listener for logging
        retryTemplate.registerListener(new RetryListener() {
            @Override
            public <T, E extends Throwable> void onError(
                RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                log.warn("Retry attempt {} failed: {}", 
                    context.getRetryCount(), throwable.getMessage());
            }

            @Override
            public <T, E extends Throwable> void close(
                RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                if (throwable != null) {
                    log.error("All retry attempts exhausted. Final error: {}", throwable.getMessage());
                } else {
                    log.info("Retry successful after {} attempts", context.getRetryCount());
                }
            }
        });

        return retryTemplate;
    }
}
