package app.recall.core;

import java.time.Instant;
import java.util.UUID;

/** The plain-data view of an appointment that the planning logic works on. */
public record UpcomingAppointment(
        UUID id,
        String customerName,
        String phone,
        Instant startsAt,
        ConsentStatus consent,
        boolean reminded) {
}
