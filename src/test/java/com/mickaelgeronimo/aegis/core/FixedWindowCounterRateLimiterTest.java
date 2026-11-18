package com.mickaelgeronimo.aegis.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class FixedWindowCounterRateLimiterTest {

    @Test
    void resetsCounterAtDiscreteWindowBoundary() {
        FakeTimeSource clock = new FakeTimeSource(1_700_000_000L);
        RateLimitConfig config = new RateLimitConfig(
                RateLimitAlgorithmType.FIXED_WINDOW_COUNTER, 2, 2, Duration.ofSeconds(10));
        RateLimiter limiter = RateLimiterFactory.create(config, clock);

        assertThat(limiter.tryAcquire().remaining()).isEqualTo(1L);
        assertThat(limiter.tryAcquire().remaining()).isZero();

        clock.advance(Duration.ofSeconds(3));
        RateLimitDecision blocked = limiter.tryAcquire();
        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.retryAfterSeconds()).isEqualTo(7L);

        clock.advance(Duration.ofSeconds(7));
        RateLimitDecision freshWindow = limiter.tryAcquire();
        assertThat(freshWindow.allowed()).isTrue();
        assertThat(freshWindow.remaining()).isEqualTo(1L);
    }
}
