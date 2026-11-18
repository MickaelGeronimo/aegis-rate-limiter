package com.mickaelgeronimo.aegis.core;

import java.time.Instant;

/**
 * Monotonic and wall-clock time abstraction enabling deterministic testing without Thread.sleep.
 */
public interface TimeSource {

    TimeSource SYSTEM = new TimeSource() {
        @Override
        public long nanoTime() {
            return System.nanoTime();
        }

        @Override
        public long epochSeconds() {
            return Instant.now().getEpochSecond();
        }
    };

    long nanoTime();

    long epochSeconds();
}
