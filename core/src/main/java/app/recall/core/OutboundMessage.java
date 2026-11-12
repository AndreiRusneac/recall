package app.recall.core;

import java.util.UUID;

/** A message that should be sent to one customer about one appointment. */
public record OutboundMessage(UUID appointmentId, String toPhone, String text) {
}
