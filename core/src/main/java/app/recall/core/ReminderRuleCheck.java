package app.recall.core;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks what an owner typed on the rules page before it becomes a rule. The point is to catch
 * mistakes that would reach patients, such as a misspelled {nume} that would be sent as it is.
 */
public final class ReminderRuleCheck {

    public static final int MIN_LEAD_HOURS = 1;
    public static final int MAX_LEAD_HOURS = 720;
    /** Keeps the whole WhatsApp link short enough for browsers, even with Romanian letters. */
    public static final int MAX_TEMPLATE_LENGTH = 600;

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^{}]*)}");

    private ReminderRuleCheck() {
    }

    public enum Problem {
        BAD_LEAD_TIME,
        EMPTY_TEMPLATE,
        TEMPLATE_TOO_LONG,
        /** {@code detail} is the offending text, for example "{nume}". */
        UNKNOWN_PLACEHOLDER
    }

    /** Either a valid {@code rule}, or a {@code problem} with its {@code detail}. */
    public record Result(ReminderRule rule, Problem problem, String detail) {

        public boolean ok() {
            return problem == null;
        }
    }

    public static Result check(int leadHours, String template) {
        if (leadHours < MIN_LEAD_HOURS || leadHours > MAX_LEAD_HOURS) {
            return problem(Problem.BAD_LEAD_TIME, String.valueOf(leadHours));
        }
        String text = template == null ? "" : template.trim();
        if (text.isEmpty()) {
            return problem(Problem.EMPTY_TEMPLATE, "");
        }
        if (text.length() > MAX_TEMPLATE_LENGTH) {
            return problem(Problem.TEMPLATE_TOO_LONG, String.valueOf(MAX_TEMPLATE_LENGTH));
        }
        Matcher matcher = PLACEHOLDER.matcher(text);
        while (matcher.find()) {
            if (!ReminderRule.PLACEHOLDERS.contains(matcher.group(1))) {
                return problem(Problem.UNKNOWN_PLACEHOLDER, matcher.group());
            }
        }
        // Anything left over after removing the good placeholders is a stray brace such as "{name".
        String leftover = PLACEHOLDER.matcher(text).replaceAll("");
        int stray = Math.max(leftover.indexOf('{'), leftover.indexOf('}'));
        if (stray >= 0) {
            return problem(Problem.UNKNOWN_PLACEHOLDER, String.valueOf(leftover.charAt(stray)));
        }
        return new Result(new ReminderRule(Duration.ofHours(leadHours), text), null, "");
    }

    private static Result problem(Problem problem, String detail) {
        return new Result(null, problem, detail);
    }
}
