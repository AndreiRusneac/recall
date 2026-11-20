package app.recall.web;

import java.time.Clock;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

/** Counts failed password logins and clears the count after a good one. */
@Component
public class LoginAttemptListener {

    private final LoginThrottles throttles;
    private final Clock clock;

    public LoginAttemptListener(LoginThrottles throttles, Clock clock) {
        this.throttles = throttles;
        this.clock = clock;
    }

    @EventListener
    void onFailure(AbstractAuthenticationFailureEvent event) {
        Authentication attempt = event.getAuthentication();
        if (attempt instanceof UsernamePasswordAuthenticationToken) {
            throttles.recordFailure(attempt.getName(), addressOf(attempt), clock.instant());
        }
    }

    @EventListener
    void onSuccess(AuthenticationSuccessEvent event) {
        Authentication login = event.getAuthentication();
        if (login instanceof UsernamePasswordAuthenticationToken) {
            throttles.recordSuccess(login.getName(), addressOf(login));
        }
    }

    private static String addressOf(Authentication authentication) {
        return authentication.getDetails() instanceof WebAuthenticationDetails details
                ? details.getRemoteAddress()
                : "unknown";
    }
}
