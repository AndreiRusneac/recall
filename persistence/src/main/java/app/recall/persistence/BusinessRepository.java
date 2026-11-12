package app.recall.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepository extends JpaRepository<BusinessEntity, UUID> {

    /** Temporary: until login exists, the dashboard shows the oldest business. */
    Optional<BusinessEntity> findFirstByOrderByCreatedAtAsc();
}
