package app.recall.web;

import app.recall.core.LoginThrottle;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * The two limits on login attempts. One per email and address together, so someone guessing a
 * password gets stopped, and a looser one per address alone, so one machine cannot try many
 * emails. Keying the first on the address as well means a stranger cannot lock a real clinic
 * out of its own account from somewhere else.
 */
@Component
public class LoginThrottles {

    static final Duration WINDOW = Duration.ofMinutes(15);

    private final LoginThrottle perAccount = new LoginThrottle(5, WINDOW);
    private final LoginThrottle perAddress = new LoginThrottle(20, WINDOW);

    public boolean isBlocked(String email, String address, Instant now) {
        return perAccount.isBlocked(accountKey(email, address), now) || perAddress.isBlocked(address, now);
    }

    public void recordFailure(String email, String address, Instant now) {
        perAccount.recordFailure(accountKey(email, address), now);
        perAddress.recordFailure(address, now);
    }

    /** Only the account count is cleared: an address that also failed elsewhere keeps its record. */
    public void recordSuccess(String email, String address) {
        perAccount.reset(accountKey(email, address));
    }

    private static String accountKey(String email, String address) {
        return email.trim().toLowerCase(Locale.ROOT) + "|" + address;
    }
}
