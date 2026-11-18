package com.mickaelgeronimo.aegis.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class LeakyBucketRateLimiterTest {

    @Test
    void drainsQueuedWaterLevelAtSteadyRate() {
        FakeTimeSource clock = new FakeTimeSource(1_700_000_000L);
        RateLimitConfig config = new RateLimitConfig(
                RateLimitAlgorithmType.LEAKY_BUCKET, 4, 2, Duration.ofSeconds(2));
        RateLimiter limiter = RateLimiterFactory.create(config, clock);

        assertThat(limiter.tryAcquire(4).allowed()).isTrue();

        RateLimitDecision overflow = limiter.tryAcquire(1);
        assertThat(overflow.allowed()).isFalse();
        assertThat(overflow.retryAfterSeconds()).isEqualTo(1L);

        clock.advance(Duration.ofSeconds(1));
        RateLimitDecision afterDrain = limiter.tryAcquire(1);
        assertThat(afterDrain.allowed()).isTrue();
        assertThat(afterDrain.remaining()).isZero();
    }
}
