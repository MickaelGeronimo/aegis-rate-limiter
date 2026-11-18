package com.mickaelgeronimo.aegis.core;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterConcurrencyTest {

    @ParameterizedTest
    @EnumSource(RateLimitAlgorithmType.class)
    void enforcesExactCapacityUnderHighConcurrency(RateLimitAlgorithmType algorithm) throws InterruptedException {
        FakeTimeSource frozenClock = new FakeTimeSource(1_700_000_000L);
        int capacity = 25;
        int threads = 16;
        int requestsPerThread = 10;
        int totalRequests = threads * requestsPerThread;
        RateLimitConfig config = new RateLimitConfig(algorithm, capacity, capacity, Duration.ofSeconds(60));
        RateLimiter limiter = RateLimiterFactory.create(config, frozenClock);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger allowedCount = new AtomicInteger();
        AtomicInteger rejectedCount = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    for (int j = 0; j < requestsPerThread; j++) {
                        if (limiter.tryAcquire().allowed()) {
                            allowedCount.incrementAndGet();
                        } else {
                            rejectedCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        assertThat(allowedCount.get()).isEqualTo(capacity);
        assertThat(rejectedCount.get()).isEqualTo(totalRequests - capacity);
    }
}
