package app.recall.web;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/** The logged-in person. Carries the id of their business, so every page knows whose data to show. */
public class RecallUser extends User {

    private final UUID businessId;

    public RecallUser(String email, String passwordHash, UUID businessId) {
        super(email, passwordHash, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        this.businessId = businessId;
    }

    public UUID getBusinessId() {
        return businessId;
    }
}
