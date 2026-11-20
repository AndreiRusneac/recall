package app.recall.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.recall.persistence.AppUserEntity;
import app.recall.persistence.AppUserRepository;
import app.recall.persistence.BusinessEntity;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Real login chain, real throttle. Only the user lookup and the clock are replaced. */
// The throttle remembers failures in memory, so every test gets a fresh application context.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WebMvcTest
@Import({SecurityConfig.class, LoginController.class, RecallUserDetailsService.class,
        LoginThrottles.class, LoginAttemptListener.class})
class LoginRateLimitTest {

    private static final Instant START = Instant.parse("2026-11-24T08:00:00Z");
    private static final String EMAIL = "owner@example.test";
    private static final String PASSWORD = "right-password";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PasswordEncoder encoder;

    @MockitoBean
    private AppUserRepository users;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void aKnownUser() {
        BusinessEntity business = mock(BusinessEntity.class);
        when(business.getId()).thenReturn(UUID.randomUUID());
        AppUserEntity user = mock(AppUserEntity.class);
        when(user.getEmail()).thenReturn(EMAIL);
        when(user.getPasswordHash()).thenReturn(encoder.encode(PASSWORD));
        when(user.getBusiness()).thenReturn(business);
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(clock.instant()).thenReturn(START);
    }

    private ResultActions login(String password, String address) throws Exception {
        return mvc.perform(post("/login").with(csrf())
                .with(request -> {
                    request.setRemoteAddr(address);
                    return request;
                })
                .param("username", EMAIL)
                .param("password", password));
    }

    private void failTimes(int times, String address) throws Exception {
        for (int i = 0; i < times; i++) {
            login("wrong", address).andExpect(header().string("Location", "/login?error"));
        }
    }

    @Test
    void aGoodPasswordWorksNormally() throws Exception {
        login(PASSWORD, "10.0.0.1").andExpect(header().string("Location", "/"));
    }

    @Test
    void afterFiveFailuresEvenTheRightPasswordIsRefused() throws Exception {
        failTimes(5, "10.0.0.1");

        login(PASSWORD, "10.0.0.1")
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/login?locked"));
    }

    @Test
    void theOwnerCanStillLogInFromAnotherAddressWhileSomeoneElseIsLockedOut() throws Exception {
        failTimes(5, "10.0.0.66");

        login(PASSWORD, "10.0.0.1").andExpect(header().string("Location", "/"));
    }

    @Test
    void aGoodLoginResetsTheCount() throws Exception {
        failTimes(4, "10.0.0.1");
        login(PASSWORD, "10.0.0.1").andExpect(header().string("Location", "/"));
        failTimes(4, "10.0.0.1");

        login(PASSWORD, "10.0.0.1").andExpect(header().string("Location", "/"));
    }

    @Test
    void theLockEndsAfterFifteenMinutes() throws Exception {
        failTimes(5, "10.0.0.1");
        login(PASSWORD, "10.0.0.1").andExpect(header().string("Location", "/login?locked"));

        when(clock.instant()).thenReturn(START.plus(Duration.ofMinutes(15)));

        login(PASSWORD, "10.0.0.1").andExpect(header().string("Location", "/"));
    }

    @Test
    void oneAddressTryingManyAccountsIsStoppedToo() throws Exception {
        for (int i = 0; i < 20; i++) {
            mvc.perform(post("/login").with(csrf())
                    .with(request -> {
                        request.setRemoteAddr("10.0.0.9");
                        return request;
                    })
                    .param("username", "nobody" + i + "@example.test")
                    .param("password", "guess"));
        }

        login(PASSWORD, "10.0.0.9").andExpect(header().string("Location", "/login?locked"));
    }
}
