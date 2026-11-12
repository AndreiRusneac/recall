package app.recall.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReminderPlannerTest {

    private static final ZoneId BUCHAREST = ZoneId.of("Europe/Bucharest");
    private static final Instant NOW = Instant.parse("2026-11-16T12:00:00Z");
    private static final ReminderRule DAY_BEFORE = new ReminderRule(
            Duration.ofHours(24),
            "Hi {name}, you have an appointment at {business} on {date} at {time}.");

    private final ReminderPlanner planner = new ReminderPlanner();

    private static UpcomingAppointment appointment(Instant startsAt, ConsentStatus consent, boolean reminded) {
        return new UpcomingAppointment(UUID.randomUUID(), "Maria", "0700 000 001", startsAt, consent, reminded);
    }

    @Test
    void plansAReminderInsideTheLeadTime() {
        UpcomingAppointment tomorrow = appointment(NOW.plus(Duration.ofHours(20)), ConsentStatus.GRANTED, false);

        List<OutboundMessage> messages = planner.plan("Demo Clinic", BUCHAREST, DAY_BEFORE, List.of(tomorrow), NOW);

        assertEquals(1, messages.size());
        assertEquals(tomorrow.id(), messages.get(0).appointmentId());
        // 12:00 UTC plus 20 hours is 08:00 UTC the next day, which is 10:00 in Bucharest in winter.
        assertEquals(
                "Hi Maria, you have an appointment at Demo Clinic on 17.11.2026 at 10:00.",
                messages.get(0).text());
    }

    @Test
    void skipsAppointmentsThatAreTooFarAway() {
        UpcomingAppointment nextWeek = appointment(NOW.plus(Duration.ofDays(6)), ConsentStatus.GRANTED, false);

        assertTrue(planner.plan("Demo Clinic", BUCHAREST, DAY_BEFORE, List.of(nextWeek), NOW).isEmpty());
    }

    @Test
    void skipsAppointmentsInThePast() {
        UpcomingAppointment earlier = appointment(NOW.minus(Duration.ofHours(1)), ConsentStatus.GRANTED, false);

        assertTrue(planner.plan("Demo Clinic", BUCHAREST, DAY_BEFORE, List.of(earlier), NOW).isEmpty());
    }

    @Test
    void neverMessagesWithoutConsent() {
        Instant soon = NOW.plus(Duration.ofHours(20));
        List<UpcomingAppointment> appointments = List.of(
                appointment(soon, ConsentStatus.UNKNOWN, false),
                appointment(soon, ConsentStatus.OPTED_OUT, false));

        assertTrue(planner.plan("Demo Clinic", BUCHAREST, DAY_BEFORE, appointments, NOW).isEmpty());
    }

    @Test
    void doesNotRemindTwice() {
        UpcomingAppointment alreadyReminded = appointment(NOW.plus(Duration.ofHours(20)), ConsentStatus.GRANTED, true);

        assertTrue(planner.plan("Demo Clinic", BUCHAREST, DAY_BEFORE, List.of(alreadyReminded), NOW).isEmpty());
    }
}
