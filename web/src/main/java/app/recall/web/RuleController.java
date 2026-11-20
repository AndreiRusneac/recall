package app.recall.web;

import app.recall.core.ReminderRule;
import app.recall.core.ReminderRuleCheck;
import app.recall.core.RuleKind;
import app.recall.core.TemplateRenderer;
import app.recall.persistence.BusinessEntity;
import app.recall.persistence.BusinessRepository;
import app.recall.persistence.RuleEntity;
import app.recall.persistence.RuleRepository;
import app.recall.verticals.VerticalDefaults;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** The page where an owner changes when reminders go out and what they say. */
@Controller
public class RuleController {

    private final BusinessRepository businesses;
    private final RuleRepository rules;

    public RuleController(BusinessRepository businesses, RuleRepository rules) {
        this.businesses = businesses;
        this.rules = rules;
    }

    @GetMapping("/rules")
    public String show(@AuthenticationPrincipal RecallUser user, Model model) {
        BusinessEntity business = businesses.findById(user.getBusinessId()).orElseThrow();
        ReminderRule current = rules.reminderRuleFor(business, VerticalDefaults.reminderRule(business.getVertical()));
        boolean custom = !rules.findUsable(business.getId(), RuleKind.REMINDER).isEmpty();
        fill(model, business, String.valueOf(current.leadTime().toHours()), current.template(), custom);
        return "rules";
    }

    @PostMapping("/rules")
    @Transactional
    public String save(
            @AuthenticationPrincipal RecallUser user,
            @RequestParam String leadHours,
            @RequestParam String template,
            Model model) {
        BusinessEntity business = businesses.findById(user.getBusinessId()).orElseThrow();

        ReminderRuleCheck.Result checked = ReminderRuleCheck.check(parseHours(leadHours), template);
        if (!checked.ok()) {
            model.addAttribute("error", describe(checked));
            boolean custom = !rules.findUsable(business.getId(), RuleKind.REMINDER).isEmpty();
            fill(model, business, leadHours, template, custom);
            return "rules";
        }

        // Replace, so there is always exactly one rule of this kind for the business.
        rules.deleteByBusinessIdAndKind(business.getId(), RuleKind.REMINDER);
        rules.save(new RuleEntity(
                business,
                RuleKind.REMINDER,
                (int) checked.rule().leadTime().toMinutes(),
                checked.rule().template(),
                true));
        return "redirect:/rules?saved";
    }

    @PostMapping("/rules/reset")
    @Transactional
    public String reset(@AuthenticationPrincipal RecallUser user) {
        rules.deleteByBusinessIdAndKind(user.getBusinessId(), RuleKind.REMINDER);
        return "redirect:/rules?reset";
    }

    private void fill(Model model, BusinessEntity business, String leadHours, String template, boolean custom) {
        model.addAttribute("leadHours", leadHours);
        model.addAttribute("template", template);
        model.addAttribute("custom", custom);
        model.addAttribute("minHours", ReminderRuleCheck.MIN_LEAD_HOURS);
        model.addAttribute("maxHours", ReminderRuleCheck.MAX_LEAD_HOURS);
        model.addAttribute("maxLength", ReminderRuleCheck.MAX_TEMPLATE_LENGTH);
        // An example with invented values, so the owner sees what a patient would read.
        model.addAttribute("preview", TemplateRenderer.render(template, Map.of(
                "name", "Maria Ionescu",
                "business", business.getName(),
                "date", "24.11.2026",
                "time", "10:00")));
    }

    /** Anything that is not a whole number becomes 0, which the check then rejects. */
    private static int parseHours(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static String describe(ReminderRuleCheck.Result result) {
        return switch (result.problem()) {
            case BAD_LEAD_TIME -> "Numărul de ore trebuie să fie între " + ReminderRuleCheck.MIN_LEAD_HOURS
                    + " și " + ReminderRuleCheck.MAX_LEAD_HOURS + ".";
            case EMPTY_TEMPLATE -> "Textul mesajului nu poate fi gol.";
            case TEMPLATE_TOO_LONG -> "Textul este prea lung. Maximum " + result.detail() + " de caractere.";
            case UNKNOWN_PLACEHOLDER -> "„" + result.detail() + "” nu este un cuvânt cheie valid. "
                    + "Folosiți doar {name}, {business}, {date} și {time}.";
        };
    }
}
