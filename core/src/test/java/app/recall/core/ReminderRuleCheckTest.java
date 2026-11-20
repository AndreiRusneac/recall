package app.recall.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ReminderRuleCheckTest {

    @Test
    void acceptsAGoodRuleAndTrimsTheText() {
        var result = ReminderRuleCheck.check(3, "  Bună ziua, {name}! {business}, {date}, {time}.  ");

        assertTrue(result.ok());
        assertEquals(Duration.ofHours(3), result.rule().leadTime());
        assertEquals("Bună ziua, {name}! {business}, {date}, {time}.", result.rule().template());
    }

    @Test
    void aTemplateWithoutPlaceholdersIsFine() {
        assertTrue(ReminderRuleCheck.check(24, "Vă așteptăm mâine.").ok());
    }

    @Test
    void rejectsALeadTimeOutsideTheRange() {
        assertEquals(ReminderRuleCheck.Problem.BAD_LEAD_TIME, ReminderRuleCheck.check(0, "Hi {name}").problem());
        assertEquals(ReminderRuleCheck.Problem.BAD_LEAD_TIME, ReminderRuleCheck.check(721, "Hi {name}").problem());
        assertTrue(ReminderRuleCheck.check(720, "Hi {name}").ok());
    }

    @Test
    void rejectsAnEmptyTemplate() {
        assertEquals(ReminderRuleCheck.Problem.EMPTY_TEMPLATE, ReminderRuleCheck.check(24, "   ").problem());
        assertEquals(ReminderRuleCheck.Problem.EMPTY_TEMPLATE, ReminderRuleCheck.check(24, null).problem());
    }

    @Test
    void rejectsATemplateThatIsTooLong() {
        var result = ReminderRuleCheck.check(24, "a".repeat(ReminderRuleCheck.MAX_TEMPLATE_LENGTH + 1));

        assertEquals(ReminderRuleCheck.Problem.TEMPLATE_TOO_LONG, result.problem());
    }

    @Test
    void rejectsAMisspelledPlaceholderSoItIsNeverSentAsText() {
        var result = ReminderRuleCheck.check(24, "Bună ziua, {nume}!");

        assertFalse(result.ok());
        assertEquals(ReminderRuleCheck.Problem.UNKNOWN_PLACEHOLDER, result.problem());
        assertEquals("{nume}", result.detail());
    }

    @Test
    void rejectsStrayBraces() {
        assertEquals("{", ReminderRuleCheck.check(24, "Bună ziua, {name").detail());
        assertEquals("}", ReminderRuleCheck.check(24, "Bună ziua, name}").detail());
    }

    @Test
    void theAllowedPlaceholdersAreExactlyTheOnesThePlannerFills() {
        assertEquals(java.util.Set.of("name", "business", "date", "time"), ReminderRule.PLACEHOLDERS);
    }
}
