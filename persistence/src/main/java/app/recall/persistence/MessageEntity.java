package app.recall.persistence;

import app.recall.core.MessageChannel;
import app.recall.core.MessageStatus;
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
import java.time.Instant;
import java.util.UUID;

/**
 * One message that left Recall, kept as a log entry: it is written once and never changed.
 * Outcomes (a confirmed or recovered appointment) will point back to these rows.
 */
@Entity
@Table(name = "message")
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private BusinessEntity business;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    // Nullable in the schema: later messages may not be about an appointment.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private EventEntity event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageChannel channel;

    @Column(nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status;

    /** When the message was handed on: to WhatsApp on the owner's device, or to a sender. */
    @Column(name = "sent_at")
    private Instant sentAt;

    // Filled in by the database default, so JPA never writes it.
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected MessageEntity() {
        // Required by JPA.
    }

    /** A message about an appointment. The business and customer are taken from the event, so they always match. */
    public MessageEntity(EventEntity event, MessageChannel channel, String body, MessageStatus status, Instant sentAt) {
        this.business = event.getBusiness();
        this.customer = event.getCustomer();
        this.event = event;
        this.channel = channel;
        this.body = body;
        this.status = status;
        this.sentAt = sentAt;
    }

    public UUID getId() {
        return id;
    }

    public BusinessEntity getBusiness() {
        return business;
    }

    public CustomerEntity getCustomer() {
        return customer;
    }

    public EventEntity getEvent() {
        return event;
    }

    public MessageChannel getChannel() {
        return channel;
    }

    public String getBody() {
        return body;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
