package app.recall.core;

import java.time.Duration;

/**
 * "Send this text this long before the appointment."
 * The template may use {name}, {business}, {date} and {time}.
 */
public record ReminderRule(Duration leadTime, String template) {

    /** The only placeholders a template may use. {@link ReminderPlanner} fills exactly these. */
    public static final java.util.Set<String> PLACEHOLDERS = java.util.Set.of("name", "business", "date", "time");

    public ReminderRule {
        if (leadTime == null || leadTime.isZero() || leadTime.isNegative()) {
            throw new IllegalArgumentException("A reminder needs a lead time longer than zero");
        }
        if (template == null || template.isBlank()) {
            throw new IllegalArgumentException("A reminder needs a template");
        }
    }
}
