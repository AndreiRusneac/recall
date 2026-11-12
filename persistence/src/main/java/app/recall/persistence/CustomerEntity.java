package app.recall.persistence;

import app.recall.core.ConsentStatus;
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
@Table(name = "customer")
public class CustomerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private BusinessEntity business;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConsentStatus consent;

    @Column(name = "last_visit_at")
    private Instant lastVisitAt;

    protected CustomerEntity() {
        // Required by JPA.
    }

    public CustomerEntity(BusinessEntity business, String name, String phone, ConsentStatus consent) {
        this.business = business;
        this.name = name;
        this.phone = phone;
        this.consent = consent;
    }

    public UUID getId() {
        return id;
    }

    public BusinessEntity getBusiness() {
        return business;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public ConsentStatus getConsent() {
        return consent;
    }

    public Instant getLastVisitAt() {
        return lastVisitAt;
    }
}
