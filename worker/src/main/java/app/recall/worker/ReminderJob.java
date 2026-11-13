package app.recall.worker;

import app.recall.core.EventKind;
import app.recall.core.EventStatus;
import app.recall.core.MessageChannel;
import app.recall.core.MessageSender;
import app.recall.core.MessageStatus;
import app.recall.core.OutboundMessage;
import app.recall.core.ReminderPlanner;
import app.recall.core.ReminderRule;
import app.recall.persistence.BusinessEntity;
import app.recall.persistence.BusinessRepository;
import app.recall.persistence.EventEntity;
import app.recall.persistence.EventRepository;
import app.recall.persistence.MessageEntity;
import app.recall.persistence.MessageRepository;
import app.recall.verticals.VerticalDefaults;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Automatic sending: every few minutes, find the reminders that are due and send them.
 *
 * It is switched off by default (recall.worker.enabled=false) because the pilot uses the
 * tap-to-send button on the dashboard instead. With the logging sender it is safe to
 * switch on locally to watch the whole loop run.
 */
@Component
@ConditionalOnProperty(name = "recall.worker.enabled", havingValue = "true")
public class ReminderJob {

    private final BusinessRepository businesses;
    private final EventRepository events;
    private final MessageRepository messages;
    private final MessageSender sender;
    private final Clock clock;
    private final ReminderPlanner planner = new ReminderPlanner();

    public ReminderJob(
            BusinessRepository businesses,
            EventRepository events,
            MessageRepository messages,
            MessageSender sender,
            Clock clock) {
        this.businesses = businesses;
        this.events = events;
        this.messages = messages;
        this.sender = sender;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${recall.worker.interval:PT5M}")
    @Transactional
    public void sendDueReminders() {
        Instant now = clock.instant();
        for (BusinessEntity business : businesses.findAll()) {
            ReminderRule rule = VerticalDefaults.reminderRule(business.getVertical());

            // Only appointments inside the lead time can be due, so that is all we load.
            List<EventEntity> candidates = events.findStartingBetween(
                    business.getId(),
                    EventKind.APPOINTMENT,
                    EventStatus.CANCELLED,
                    now,
                    now.plus(rule.leadTime()));
            Map<UUID, EventEntity> byId = candidates.stream()
                    .collect(Collectors.toMap(EventEntity::getId, Function.identity()));

            List<OutboundMessage> due = planner.plan(
                    business.getName(),
                    ZoneId.of(business.getTimezone()),
                    rule,
                    candidates.stream().map(EventEntity::toUpcoming).toList(),
                    now);

            for (OutboundMessage message : due) {
                // Send first: if the sender throws, nothing below runs and the transaction rolls back,
                // so we never record a message that did not go out.
                sender.send(message);
                EventEntity event = byId.get(message.appointmentId());
                // The entity is managed inside this transaction, so the change is saved on commit.
                event.markReminderSent(now);
                messages.save(new MessageEntity(event, MessageChannel.WHATSAPP_API, message.text(), MessageStatus.SENT, now));
            }
        }
    }
}
