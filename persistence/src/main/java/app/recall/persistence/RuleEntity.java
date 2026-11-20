package app.recall.persistence;

import app.recall.core.ReminderRule;
import app.recall.core.RuleKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** One business's own rule, for example "remind 3 hours before, with this wording". */
@Entity
@Table(name = "rule")
public class RuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private BusinessEntity business;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleKind kind;

    @Column(name = "lead_time_minutes", nullable = false)
    private int leadTimeMinutes;

    @Column(nullable = false)
    private String template;

    @Column(nullable = false)
    private boolean enabled;

    // Filled in by the database default, so JPA never writes it.
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected RuleEntity() {
        // Required by JPA.
    }

    public RuleEntity(BusinessEntity business, RuleKind kind, int leadTimeMinutes, String template, boolean enabled) {
        this.business = business;
        this.kind = kind;
        this.leadTimeMinutes = leadTimeMinutes;
        this.template = template;
        this.enabled = enabled;
    }

    /** Only call this on a row the repository returned: it is already known to be usable. */
    public ReminderRule toReminderRule() {
        return new ReminderRule(Duration.ofMinutes(leadTimeMinutes), template);
    }

    public UUID getId() {
        return id;
    }

    public BusinessEntity getBusiness() {
        return business;
    }

    public RuleKind getKind() {
        return kind;
    }

    public int getLeadTimeMinutes() {
        return leadTimeMinutes;
    }

    public String getTemplate() {
        return template;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
