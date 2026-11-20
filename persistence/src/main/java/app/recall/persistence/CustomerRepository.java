package app.recall.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID> {

    /** The schema allows one customer per phone number within a business. */
    Optional<CustomerEntity> findByBusinessIdAndPhone(UUID businessId, String phone);
}
