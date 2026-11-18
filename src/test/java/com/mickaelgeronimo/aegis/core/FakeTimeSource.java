package com.mickaelgeronimo.aegis.core;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe controllable clock for deterministic rate-limiter tests.
 */
final class FakeTimeSource implements TimeSource {

    private final AtomicLong nanos = new AtomicLong(0L);
    private final long baseEpochSeconds;

    FakeTimeSource(long baseEpochSeconds) {
        this.baseEpochSeconds = baseEpochSeconds;
    }

    @Override
    public long nanoTime() {
        return nanos.get();
    }

    @Override
    public long epochSeconds() {
        return baseEpochSeconds + (nanos.get() / 1_000_000_000L);
    }

    void advance(Duration duration) {
        nanos.addAndGet(duration.toNanos());
    }
}
