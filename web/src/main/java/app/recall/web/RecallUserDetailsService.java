package app.recall.web;

import app.recall.persistence.AppUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Lets Spring Security look up a login in the app_user table. */
@Service
public class RecallUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    public RecallUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return users.findByEmail(email)
                .map(user -> new RecallUser(user.getEmail(), user.getPasswordHash(), user.getBusiness().getId()))
                .orElseThrow(() -> new UsernameNotFoundException("No login for this email"));
    }
}
