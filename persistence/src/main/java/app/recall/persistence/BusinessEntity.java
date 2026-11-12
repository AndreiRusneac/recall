package app.recall.persistence;

import app.recall.core.Plan;
import app.recall.core.Vertical;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "business")
public class BusinessEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Vertical vertical;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plan plan;

    @Column(nullable = false)
    private String timezone;

    // Filled in by the database default, so JPA never writes it.
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected BusinessEntity() {
        // Required by JPA.
    }

    public BusinessEntity(String name, Vertical vertical, Plan plan, String timezone) {
        this.name = name;
        this.vertical = vertical;
        this.plan = plan;
        this.timezone = timezone;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Vertical getVertical() {
        return vertical;
    }

    public Plan getPlan() {
        return plan;
    }

    public String getTimezone() {
        return timezone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
