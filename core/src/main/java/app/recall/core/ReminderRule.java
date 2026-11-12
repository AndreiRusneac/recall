package app.recall.core;

import java.time.Duration;

/**
 * "Send this text this long before the appointment."
 * The template may use {name}, {business}, {date} and {time}.
 */
public record ReminderRule(Duration leadTime, String template) {
}
