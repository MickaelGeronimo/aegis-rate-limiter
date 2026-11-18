package com.mickaelgeronimo.aegis.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SlidingWindowLogRateLimiterTest {

    @Test
    void evictsOnlyExpiredTimestampsInsideSlidingWindow() {
        FakeTimeSource clock = new FakeTimeSource(1_700_000_000L);
        RateLimitConfig config = new RateLimitConfig(
                RateLimitAlgorithmType.SLIDING_WINDOW_LOG, 3, 3, Duration.ofSeconds(10));
        RateLimiter limiter = RateLimiterFactory.create(config, clock);

        assertThat(limiter.tryAcquire().allowed()).isTrue();
        clock.advance(Duration.ofSeconds(4));
        assertThat(limiter.tryAcquire().allowed()).isTrue();
        clock.advance(Duration.ofSeconds(2));
        assertThat(limiter.tryAcquire().allowed()).isTrue();

        RateLimitDecision rejected = limiter.tryAcquire();
        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfterSeconds()).isEqualTo(4L);

        clock.advance(Duration.ofSeconds(4));
        RateLimitDecision admitted = limiter.tryAcquire();
        assertThat(admitted.allowed()).isTrue();
        assertThat(admitted.remaining()).isZero();
    }
}
