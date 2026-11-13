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
                    "Bună ziua {name}, vă reamintim programarea la {business} în data de {date}, ora {time}. "
                            + "Răspundeți cu 1 pentru confirmare sau 2 pentru anulare.");
            case SALON -> new ReminderRule(
                    Duration.ofHours(24),
                    "Bună ziua {name}, vă așteptăm la {business} în data de {date}, ora {time}. "
                            + "Răspundeți cu 1 pentru confirmare sau 2 dacă nu puteți ajunge.");
            case AUTO_SERVICE -> new ReminderRule(
                    Duration.ofHours(48),
                    "Bună ziua {name}, mașina dumneavoastră este programată la {business} în data de {date}, ora {time}. "
                            + "Răspundeți cu 1 pentru confirmare sau 2 pentru reprogramare.");
        };
    }
}
