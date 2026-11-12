package app.recall.core;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Decides which reminders are due. Pure logic: data in, messages out.
 * It never reads a clock, a database or the network, so it is easy to test.
 */
public final class ReminderPlanner {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    /** Returns one message for every appointment whose reminder is due at {@code now}. */
    public List<OutboundMessage> plan(
            String businessName,
            ZoneId zone,
            ReminderRule rule,
            List<UpcomingAppointment> appointments,
            Instant now) {
        return appointments.stream()
                .filter(appointment -> isDue(appointment, rule, now))
                .map(appointment -> render(businessName, zone, rule, appointment))
                .toList();
    }

    /**
     * A reminder is due when the customer consented, has not been reminded yet,
     * the appointment is still in the future, and we are inside the rule's lead time.
     */
    public boolean isDue(UpcomingAppointment appointment, ReminderRule rule, Instant now) {
        boolean insideLeadTime = !appointment.startsAt().minus(rule.leadTime()).isAfter(now);
        return appointment.consent() == ConsentStatus.GRANTED
                && !appointment.reminded()
                && appointment.startsAt().isAfter(now)
                && insideLeadTime;
    }

    /** Builds the reminder text for one appointment, whether or not it is due. */
    public OutboundMessage render(
            String businessName,
            ZoneId zone,
            ReminderRule rule,
            UpcomingAppointment appointment) {
        ZonedDateTime local = appointment.startsAt().atZone(zone);
        String text = TemplateRenderer.render(rule.template(), Map.of(
                "name", appointment.customerName(),
                "business", businessName,
                "date", DATE.format(local),
                "time", TIME.format(local)));
        return new OutboundMessage(appointment.id(), appointment.phone(), text);
    }
}
