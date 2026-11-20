package app.recall.persistence;

import app.recall.core.ReminderRule;
import app.recall.core.RuleKind;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RuleRepository extends JpaRepository<RuleEntity, UUID> {

    /**
     * Rules a business can actually use: enabled, with a lead time above zero and some text.
     * A badly filled-in row is skipped here instead of breaking every page that needs a rule.
     * The newest row comes first.
     */
    @Query("""
            select r from RuleEntity r
            where r.business.id = :businessId
              and r.kind = :kind
              and r.enabled = true
              and r.leadTimeMinutes > 0
              and trim(r.template) <> ''
            order by r.createdAt desc
            """)
    List<RuleEntity> findUsable(@Param("businessId") UUID businessId, @Param("kind") RuleKind kind);

    /** Removes every rule of this kind for the business, so it goes back to the vertical defaults. */
    void deleteByBusinessIdAndKind(UUID businessId, RuleKind kind);

    /**
     * The reminder rule to use for a business: its own if it has a usable one, otherwise
     * {@code fallback} (the default for its kind of business). The dashboard, the send button and
     * the worker all call this, so they can never disagree about what is due.
     */
    default ReminderRule reminderRuleFor(BusinessEntity business, ReminderRule fallback) {
        return findUsable(business.getId(), RuleKind.REMINDER).stream()
                .findFirst()
                .map(RuleEntity::toReminderRule)
                .orElse(fallback);
    }
}
