package app.recall.core;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Slows down password guessing: after {@code maxFailures} failures inside {@code window}, a key
 * (for example an email plus an address) is blocked until its oldest failure leaves the window.
 * Pure logic: the caller passes the time in. Kept in memory, so it covers one running copy of the
 * app and forgets everything on restart, which is enough for a pilot.
 */
public final class LoginThrottle {

    /** Above this many tracked keys, old entries are swept so the map cannot grow without limit. */
    private static final int SWEEP_ABOVE = 10_000;

    private final int maxFailures;
    private final Duration window;
    private final Map<String, Deque<Instant>> failures = new HashMap<>();

    public LoginThrottle(int maxFailures, Duration window) {
        if (maxFailures < 1 || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("A throttle needs at least one failure and a positive window");
        }
        this.maxFailures = maxFailures;
        this.window = window;
    }

    public synchronized boolean isBlocked(String key, Instant now) {
        Deque<Instant> recent = failures.get(key);
        if (recent == null) {
            return false;
        }
        dropExpired(recent, now);
        if (recent.isEmpty()) {
            failures.remove(key);
            return false;
        }
        return recent.size() >= maxFailures;
    }

    public synchronized void recordFailure(String key, Instant now) {
        if (failures.size() > SWEEP_ABOVE) {
            sweep(now);
        }
        Deque<Instant> recent = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        dropExpired(recent, now);
        recent.addLast(now);
    }

    /** A successful login clears the count for that key. */
    public synchronized void reset(String key) {
        failures.remove(key);
    }

    private void dropExpired(Deque<Instant> recent, Instant now) {
        Instant cutoff = now.minus(window);
        while (!recent.isEmpty() && !recent.peekFirst().isAfter(cutoff)) {
            recent.removeFirst();
        }
    }

    private void sweep(Instant now) {
        failures.values().forEach(recent -> dropExpired(recent, now));
        failures.values().removeIf(Deque::isEmpty);
    }
}
