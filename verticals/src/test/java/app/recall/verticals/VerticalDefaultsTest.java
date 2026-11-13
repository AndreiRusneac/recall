package app.recall.verticals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.recall.core.ReminderRule;
import app.recall.core.TemplateRenderer;
import app.recall.core.Vertical;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class VerticalDefaultsTest {

    @ParameterizedTest
    @EnumSource(Vertical.class)
    void everyVerticalKeepsAllPlaceholders(Vertical vertical) {
        String template = VerticalDefaults.reminderRule(vertical).template();

        assertTrue(template.contains("{name}"), "missing {name}");
        assertTrue(template.contains("{business}"), "missing {business}");
        assertTrue(template.contains("{date}"), "missing {date}");
        assertTrue(template.contains("{time}"), "missing {time}");
    }

    @ParameterizedTest
    @EnumSource(Vertical.class)
    void renderingLeavesNoUnfilledPlaceholder(Vertical vertical) {
        String rendered = TemplateRenderer.render(
                VerticalDefaults.reminderRule(vertical).template(),
                Map.of(
                        "name", "Maria",
                        "business", "Demo Clinic",
                        "date", "17.11.2026",
                        "time", "10:00"));

        assertFalse(rendered.contains("{"), "unfilled placeholder in: " + rendered);
        assertFalse(rendered.contains("}"), "unfilled placeholder in: " + rendered);
    }

    @Test
    void leadTimesMatchTheAgreedDefaults() {
        assertEquals(Duration.ofHours(24), VerticalDefaults.reminderRule(Vertical.CLINIC).leadTime());
        assertEquals(Duration.ofHours(24), VerticalDefaults.reminderRule(Vertical.SALON).leadTime());
        assertEquals(Duration.ofHours(48), VerticalDefaults.reminderRule(Vertical.AUTO_SERVICE).leadTime());
    }
}
