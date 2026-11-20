package app.recall.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import app.recall.core.Plan;
import app.recall.core.ReminderRule;
import app.recall.core.RuleKind;
import app.recall.core.Vertical;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@PostgresJpaTest
class RuleRepositoryTest {

    private static final ReminderRule DEFAULT = new ReminderRule(Duration.ofHours(24), "default {name}");

    @Autowired
    private RuleRepository rules;

    @Autowired
    private TestEntityManager entityManager;

    private BusinessEntity clinic;

    @BeforeEach
    void setUp() {
        clinic = entityManager.persist(new BusinessEntity("Test Clinic", Vertical.CLINIC, Plan.REMINDERS, "Europe/Bucharest"));
    }

    private ReminderRule resolved() {
        entityManager.flush();
        entityManager.clear();
        return rules.reminderRuleFor(clinic, DEFAULT);
    }

    @Test
    void usesTheDefaultWhenTheBusinessHasNoRule() {
        assertThat(resolved()).isEqualTo(DEFAULT);
    }

    @Test
    void usesTheBusinessOwnRule() {
        entityManager.persist(new RuleEntity(clinic, RuleKind.REMINDER, 180, "own {name}", true));

        assertThat(resolved()).isEqualTo(new ReminderRule(Duration.ofMinutes(180), "own {name}"));
    }

    @Test
    void ignoresADisabledRule() {
        entityManager.persist(new RuleEntity(clinic, RuleKind.REMINDER, 180, "own {name}", false));

        assertThat(resolved()).isEqualTo(DEFAULT);
    }

    @Test
    void ignoresABadlyFilledInRuleInsteadOfFailing() {
        entityManager.persist(new RuleEntity(clinic, RuleKind.REMINDER, 0, "zero lead time", true));
        entityManager.persist(new RuleEntity(clinic, RuleKind.REMINDER, -5, "negative lead time", true));
        entityManager.persist(new RuleEntity(clinic, RuleKind.REMINDER, 60, "   ", true));

        assertThat(resolved()).isEqualTo(DEFAULT);
    }

    @Test
    void neverUsesAnotherBusinessRule() {
        BusinessEntity other = entityManager.persist(
                new BusinessEntity("Other Clinic", Vertical.CLINIC, Plan.REMINDERS, "Europe/Bucharest"));
        entityManager.persist(new RuleEntity(other, RuleKind.REMINDER, 180, "someone else's", true));

        assertThat(resolved()).isEqualTo(DEFAULT);
    }
}
