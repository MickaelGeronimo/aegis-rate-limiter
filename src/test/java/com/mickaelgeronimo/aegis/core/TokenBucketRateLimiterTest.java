package com.mickaelgeronimo.aegis.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenBucketRateLimiterTest {

    @Test
    void allowsBurstUpToCapacityAndRefillsOverTime() {
        FakeTimeSource clock = new FakeTimeSource(1_700_000_000L);
        RateLimitConfig config = new RateLimitConfig(
                RateLimitAlgorithmType.TOKEN_BUCKET, 5, 2, Duration.ofSeconds(2));
        RateLimiter limiter = RateLimiterFactory.create(config, clock);

        assertThat(limiter.tryAcquire(3).allowed()).isTrue();
        RateLimitDecision second = limiter.tryAcquire(2);
        assertThat(second.allowed()).isTrue();
        assertThat(second.remaining()).isZero();

        RateLimitDecision blocked = limiter.tryAcquire(1);
        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.retryAfterSeconds()).isEqualTo(1L);

        clock.advance(Duration.ofSeconds(1));
        RateLimitDecision afterOneSecond = limiter.tryAcquire(1);
        assertThat(afterOneSecond.allowed()).isTrue();
        assertThat(afterOneSecond.remaining()).isZero();

        clock.advance(Duration.ofSeconds(10));
        RateLimitDecision capped = limiter.tryAcquire(5);
        assertThat(capped.allowed()).isTrue();
        assertThat(capped.remaining()).isZero();
    }

    @Test
    void rejectsRequestsExceedingBucketCapacityOrNonPositivePermits() {
        FakeTimeSource clock = new FakeTimeSource(1_700_000_000L);
        RateLimitConfig config = new RateLimitConfig(
                RateLimitAlgorithmType.TOKEN_BUCKET, 3, 3, Duration.ofSeconds(10));
        RateLimiter limiter = RateLimiterFactory.create(config, clock);

        RateLimitDecision impossible = limiter.tryAcquire(4);
        assertThat(impossible.allowed()).isFalse();
        assertThat(impossible.remaining()).isEqualTo(3L);

        assertThatThrownBy(() -> limiter.tryAcquire(0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
