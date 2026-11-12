package app.recall.web;

import app.recall.core.ConsentStatus;
import app.recall.core.OutboundMessage;
import app.recall.core.PhoneNumbers;
import app.recall.core.ReminderPlanner;
import app.recall.core.ReminderRule;
import app.recall.core.WhatsAppLink;
import app.recall.persistence.BusinessEntity;
import app.recall.persistence.EventEntity;
import app.recall.persistence.EventRepository;
import app.recall.verticals.VerticalDefaults;
import java.net.URI;
import java.time.Clock;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

/**
 * Tap-to-send: the "Send on WhatsApp" button posts here. We record that the reminder
 * went out, then redirect the browser to a WhatsApp link with the text filled in.
 */
@Controller
public class ReminderController {

    // Numbers typed without a country code are assumed to be Romanian.
    private static final String DEFAULT_COUNTRY_CODE = "40";

    private final EventRepository events;
    private final Clock clock;
    private final ReminderPlanner planner = new ReminderPlanner();

    public ReminderController(EventRepository events, Clock clock) {
        this.events = events;
        this.clock = clock;
    }

    @PostMapping("/reminders/{eventId}/send")
    @Transactional
    public ResponseEntity<Void> send(@PathVariable UUID eventId) {
        EventEntity event = events.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (event.getCustomer().getConsent() != ConsentStatus.GRANTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This customer has not agreed to messages");
        }

        BusinessEntity business = event.getBusiness();
        ReminderRule rule = VerticalDefaults.reminderRule(business.getVertical());
        OutboundMessage message = planner.render(
                business.getName(),
                ZoneId.of(business.getTimezone()),
                rule,
                event.toUpcoming());

        event.markReminderSent(clock.instant());

        String phone = PhoneNumbers.toInternational(message.toPhone(), DEFAULT_COUNTRY_CODE);
        URI whatsApp = URI.create(WhatsAppLink.to(phone, message.text()));
        return ResponseEntity.status(HttpStatus.SEE_OTHER).location(whatsApp).build();
    }
}
