package app.recall.core;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ReminderRuleTest {

    @Test
    void aRuleNeedsALeadTimeLongerThanZero() {
        assertThrows(IllegalArgumentException.class, () -> new ReminderRule(Duration.ZERO, "Hi {name}"));
        assertThrows(IllegalArgumentException.class, () -> new ReminderRule(Duration.ofHours(-1), "Hi {name}"));
    }

    @Test
    void aRuleNeedsATemplate() {
        assertThrows(IllegalArgumentException.class, () -> new ReminderRule(Duration.ofHours(24), "  "));
    }
}
