package app.recall.persistence;

import app.recall.core.EventKind;
import app.recall.core.EventStatus;
import app.recall.core.UpcomingAppointment;
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

@Entity
@Table(name = "event")
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private BusinessEntity business;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventKind kind;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @Column(name = "reminder_sent_at")
    private Instant reminderSentAt;

    protected EventEntity() {
        // Required by JPA.
    }

    public EventEntity(BusinessEntity business, CustomerEntity customer, EventKind kind, Instant startsAt) {
        this.business = business;
        this.customer = customer;
        this.kind = kind;
        this.startsAt = startsAt;
        this.status = EventStatus.SCHEDULED;
    }

    /** Converts this row into the plain record the core logic works on. */
    public UpcomingAppointment toUpcoming() {
        return new UpcomingAppointment(
                id,
                customer.getName(),
                customer.getPhone(),
                startsAt,
                customer.getConsent(),
                reminderSentAt != null);
    }

    public void markReminderSent(Instant when) {
        this.reminderSentAt = when;
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

    public EventKind getKind() {
        return kind;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public EventStatus getStatus() {
        return status;
    }

    public Instant getReminderSentAt() {
        return reminderSentAt;
    }
}
