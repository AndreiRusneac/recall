package app.recall.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUserEntity, UUID> {

    /**
     * Finds a login by email, ignoring upper and lower case. The business id is read in the same
     * query, so the login code never needs a lazy load outside a transaction.
     */
    @Query("select u from AppUserEntity u join fetch u.business where lower(u.email) = lower(:email)")
    Optional<AppUserEntity> findByEmail(@Param("email") String email);
}
