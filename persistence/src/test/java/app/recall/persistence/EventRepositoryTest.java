package app.recall.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import app.recall.core.ConsentStatus;
import app.recall.core.EventKind;
import app.recall.core.EventStatus;
import app.recall.core.Plan;
import app.recall.core.UpcomingAppointment;
import app.recall.core.Vertical;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@PostgresJpaTest
class EventRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-11-16T12:00:00Z");
    private static final Instant WEEK_LATER = NOW.plus(Duration.ofDays(7));

    @Autowired
    private EventRepository events;

    @Autowired
    private TestEntityManager entityManager;

    private BusinessEntity clinic;
    private CustomerEntity customer;

    @BeforeEach
    void setUp() {
        clinic = entityManager.persist(new BusinessEntity("Test Clinic", Vertical.CLINIC, Plan.REMINDERS, "Europe/Bucharest"));
        customer = entityManager.persist(new CustomerEntity(clinic, "Test Customer", "0700 000 001", ConsentStatus.GRANTED));
    }

    private EventEntity appointmentAt(Instant startsAt) {
        return entityManager.persist(new EventEntity(clinic, customer, EventKind.APPOINTMENT, startsAt));
    }

    private List<UUID> findIdsInNextWeek() {
        entityManager.flush();
        entityManager.clear();
        return events.findStartingBetween(clinic.getId(), EventKind.APPOINTMENT, EventStatus.CANCELLED, NOW, WEEK_LATER)
                .stream()
                .map(EventEntity::getId)
                .toList();
    }

    @Test
    void returnsAppointmentsInsideTheWindowInStartOrder() {
        EventEntity later = appointmentAt(NOW.plus(Duration.ofDays(3)));
        EventEntity sooner = appointmentAt(NOW.plus(Duration.ofHours(20)));

        assertThat(findIdsInNextWeek()).containsExactly(sooner.getId(), later.getId());
    }

    @Test
    void windowExcludesItsStartAndIncludesItsEnd() {
        appointmentAt(NOW);
        EventEntity atEnd = appointmentAt(WEEK_LATER);
        appointmentAt(WEEK_LATER.plusSeconds(1));
        appointmentAt(NOW.minus(Duration.ofHours(1)));

        assertThat(findIdsInNextWeek()).containsExactly(atEnd.getId());
    }

    @Test
    void leavesOutTheExcludedStatus() {
        EventEntity cancelled = appointmentAt(NOW.plus(Duration.ofHours(20)));
        EventEntity kept = appointmentAt(NOW.plus(Duration.ofHours(21)));
        entityManager.flush();
        // EventEntity has no way to cancel yet, so set the status directly.
        entityManager.getEntityManager()
                .createNativeQuery("update event set status = 'CANCELLED' where id = :id")
                .setParameter("id", cancelled.getId())
                .executeUpdate();

        assertThat(findIdsInNextWeek()).containsExactly(kept.getId());
    }

    @Test
    void leavesOutOtherKindsAndOtherBusinesses() {
        EventEntity appointment = appointmentAt(NOW.plus(Duration.ofHours(20)));
        entityManager.persist(new EventEntity(clinic, customer, EventKind.MISSED_CALL, NOW.plus(Duration.ofHours(20))));

        BusinessEntity otherClinic = entityManager.persist(
                new BusinessEntity("Other Clinic", Vertical.CLINIC, Plan.REMINDERS, "Europe/Bucharest"));
        CustomerEntity otherCustomer = entityManager.persist(
                new CustomerEntity(otherClinic, "Other Customer", "0700 000 002", ConsentStatus.GRANTED));
        entityManager.persist(new EventEntity(otherClinic, otherCustomer, EventKind.APPOINTMENT, NOW.plus(Duration.ofHours(20))));

        assertThat(findIdsInNextWeek()).containsExactly(appointment.getId());
    }

    @Test
    void loadsTheCustomerWithTheEvent() {
        appointmentAt(NOW.plus(Duration.ofHours(20)));
        entityManager.flush();
        entityManager.clear();

        List<EventEntity> found = events.findStartingBetween(
                clinic.getId(), EventKind.APPOINTMENT, EventStatus.CANCELLED, NOW, WEEK_LATER);
        // Detach so a lazy customer would fail instead of quietly running a second query.
        entityManager.clear();

        UpcomingAppointment upcoming = found.get(0).toUpcoming();
        assertThat(upcoming.customerName()).isEqualTo("Test Customer");
        assertThat(upcoming.consent()).isEqualTo(ConsentStatus.GRANTED);
        assertThat(upcoming.reminded()).isFalse();
    }

    @Test
    void savesWhenTheReminderWasSent() {
        EventEntity event = appointmentAt(NOW.plus(Duration.ofHours(20)));
        event.markReminderSent(NOW);
        entityManager.flush();
        entityManager.clear();

        EventEntity reloaded = events.findById(event.getId()).orElseThrow();
        assertThat(reloaded.getReminderSentAt()).isEqualTo(NOW);
        assertThat(reloaded.getStatus()).isEqualTo(EventStatus.SCHEDULED);
    }
}
