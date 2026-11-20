package app.recall.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import app.recall.core.Plan;
import app.recall.core.Vertical;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@PostgresJpaTest
class AppUserRepositoryTest {

    @Autowired
    private AppUserRepository users;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findsAUserByEmailIgnoringCaseAndLoadsTheBusiness() {
        BusinessEntity clinic = entityManager.persist(
                new BusinessEntity("Test Clinic", Vertical.CLINIC, Plan.REMINDERS, "Europe/Bucharest"));
        entityManager.persist(new AppUserEntity(clinic, "Owner@Example.test", "hash"));
        entityManager.flush();
        entityManager.clear();

        AppUserEntity found = users.findByEmail("owner@example.TEST").orElseThrow();

        // Detached on purpose: a lazy business would throw here instead of quietly querying again.
        entityManager.clear();
        assertThat(found.getBusiness().getId()).isEqualTo(clinic.getId());
        assertThat(found.getPasswordHash()).isEqualTo("hash");
    }

    @Test
    void returnsNothingForAnUnknownEmail() {
        assertThat(users.findByEmail("nobody@example.test")).isEmpty();
    }
}
