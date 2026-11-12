package app.recall.verticals;

import app.recall.core.ReminderRule;
import app.recall.core.Vertical;
import java.time.Duration;

/**
 * The rules and wording a business starts with, per kind of business.
 * Until the rule table is wired up, these defaults are what the app uses.
 */
public final class VerticalDefaults {

    private VerticalDefaults() {
    }

    public static ReminderRule reminderRule(Vertical vertical) {
        return switch (vertical) {
            case CLINIC -> new ReminderRule(
                    Duration.ofHours(24),
                    "Hi {name}, this is a reminder of your appointment at {business} on {date} at {time}. "
                            + "Reply 1 to confirm or 2 to cancel.");
            case SALON -> new ReminderRule(
                    Duration.ofHours(24),
                    "Hi {name}, see you at {business} on {date} at {time}. "
                            + "Reply 1 to confirm or 2 if you can't make it.");
            case AUTO_SERVICE -> new ReminderRule(
                    Duration.ofHours(48),
                    "Hi {name}, your car is booked in at {business} on {date} at {time}. "
                            + "Reply 1 to confirm or 2 to reschedule.");
        };
    }
}
