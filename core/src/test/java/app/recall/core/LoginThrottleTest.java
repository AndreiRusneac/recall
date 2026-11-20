package app.recall.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LoginThrottleTest {

    private static final Instant T0 = Instant.parse("2026-11-24T08:00:00Z");

    private final LoginThrottle throttle = new LoginThrottle(3, Duration.ofMinutes(15));

    private void fail(String key, int times, Instant at) {
        for (int i = 0; i < times; i++) {
            throttle.recordFailure(key, at);
        }
    }

    @Test
    void allowsAttemptsUnderTheLimit() {
        fail("a", 2, T0);

        assertFalse(throttle.isBlocked("a", T0));
    }

    @Test
    void blocksAtTheLimit() {
        fail("a", 3, T0);

        assertTrue(throttle.isBlocked("a", T0.plusSeconds(1)));
    }

    @Test
    void unblocksOnceTheFailuresLeaveTheWindow() {
        fail("a", 3, T0);

        assertTrue(throttle.isBlocked("a", T0.plus(Duration.ofMinutes(14))));
        assertFalse(throttle.isBlocked("a", T0.plus(Duration.ofMinutes(15))));
    }

    @Test
    void failuresOfOneKeyDoNotBlockAnother() {
        fail("a", 3, T0);

        assertFalse(throttle.isBlocked("b", T0));
    }

    @Test
    void aSuccessfulLoginClearsTheCount() {
        fail("a", 2, T0);
        throttle.reset("a");
        fail("a", 2, T0);

        assertFalse(throttle.isBlocked("a", T0));
    }

    @Test
    void onlyRecentFailuresCount() {
        fail("a", 2, T0);
        fail("a", 1, T0.plus(Duration.ofMinutes(20)));

        assertFalse(throttle.isBlocked("a", T0.plus(Duration.ofMinutes(20))));
    }

    @Test
    void rejectsNonsenseSettings() {
        assertThrows(IllegalArgumentException.class, () -> new LoginThrottle(0, Duration.ofMinutes(1)));
        assertThrows(IllegalArgumentException.class, () -> new LoginThrottle(3, Duration.ZERO));
    }
}
