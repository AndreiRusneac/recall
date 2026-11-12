package app.recall.persistence;

import app.recall.core.EventKind;
import app.recall.core.EventStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<EventEntity, UUID> {

    /**
     * Events of one kind for one business that start after {@code from} and no later than {@code to},
     * leaving out one status (normally CANCELLED). The customer is loaded in the same query.
     */
    @Query("""
            select e from EventEntity e
            join fetch e.customer
            where e.business.id = :businessId
              and e.kind = :kind
              and e.status <> :excluded
              and e.startsAt > :from
              and e.startsAt <= :to
            order by e.startsAt
            """)
    List<EventEntity> findStartingBetween(
            @Param("businessId") UUID businessId,
            @Param("kind") EventKind kind,
            @Param("excluded") EventStatus excluded,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
