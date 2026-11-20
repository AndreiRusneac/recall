package app.recall.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.recall.core.ConsentStatus;
import app.recall.core.EventStatus;
import app.recall.core.UpcomingAppointment;
import app.recall.core.Vertical;
import app.recall.persistence.AppUserRepository;
import app.recall.persistence.AppointmentImporter;
import app.recall.persistence.BusinessEntity;
import app.recall.persistence.BusinessRepository;
import app.recall.persistence.CustomerEntity;
import app.recall.persistence.EventEntity;
import app.recall.persistence.EventRepository;
import app.recall.persistence.MessageEntity;
import app.recall.persistence.MessageRepository;
import app.recall.persistence.RuleRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Who may see and do what. Repositories are mocked: these tests are about the rules, not the database. */
@WebMvcTest
@Import({SecurityConfig.class, ReminderController.class, DashboardController.class,
        ImportController.class, LoginController.class, AppointmentController.class})
class SecurityTest {

    private static final Instant NOW = Instant.parse("2026-11-24T08:00:00Z");

    private static final UUID MY_BUSINESS = UUID.randomUUID();
    private static final UUID OTHER_BUSINESS = UUID.randomUUID();
    private static final UUID EVENT = UUID.randomUUID();

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EventRepository events;
    @MockitoBean
    private MessageRepository messages;
    @MockitoBean
    private BusinessRepository businesses;
    @MockitoBean
    private RuleRepository rules;
    @MockitoBean
    private AppUserRepository users;
    @MockitoBean
    private AppointmentImporter importer;
    @MockitoBean
    private Clock clock;

    @BeforeEach
    void noCustomRules() {
        // A mock skips the real default method; make it behave like a business without its own rule.
        when(rules.reminderRuleFor(any(), any())).thenAnswer(call -> call.getArgument(1));
    }

    private static RecallUser me() {
        return new RecallUser("owner@example.test", "hash", MY_BUSINESS);
    }

    /** An appointment of {@code businessId} for a customer with the given consent. */
    private EventEntity appointmentOf(UUID businessId, ConsentStatus consent) {
        BusinessEntity business = mock(BusinessEntity.class);
        when(business.getId()).thenReturn(businessId);
        when(business.getName()).thenReturn("Test Clinic");
        when(business.getVertical()).thenReturn(Vertical.CLINIC);
        when(business.getTimezone()).thenReturn("Europe/Bucharest");

        CustomerEntity customer = mock(CustomerEntity.class);
        when(customer.getConsent()).thenReturn(consent);

        EventEntity event = mock(EventEntity.class);
        when(event.getBusiness()).thenReturn(business);
        when(event.getCustomer()).thenReturn(customer);
        when(event.toUpcoming()).thenReturn(new UpcomingAppointment(
                EVENT, "Test Customer", "40700000001", NOW.plusSeconds(3600), consent, false));
        when(events.findById(EVENT)).thenReturn(Optional.of(event));
        when(clock.instant()).thenReturn(NOW);
        return event;
    }

    @Test
    void theDashboardAsksYouToLogInFirst() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/login"));
    }

    @Test
    void theImportPageAsksYouToLogInFirst() throws Exception {
        mvc.perform(get("/import"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/login"));
    }

    @Test
    void theLoginPageIsOpen() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void sendingWithoutLoggingInIsRefused() throws Exception {
        appointmentOf(MY_BUSINESS, ConsentStatus.GRANTED);

        mvc.perform(post("/reminders/{id}/send", EVENT).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/login"));
        verify(messages, never()).save(any(MessageEntity.class));
    }

    @Test
    void sendingWithoutTheCsrfTokenIsRefused() throws Exception {
        appointmentOf(MY_BUSINESS, ConsentStatus.GRANTED);

        mvc.perform(post("/reminders/{id}/send", EVENT).with(user(me())))
                .andExpect(status().isForbidden());
        verify(messages, never()).save(any(MessageEntity.class));
    }

    @Test
    void youCanSendYourOwnReminder() throws Exception {
        appointmentOf(MY_BUSINESS, ConsentStatus.GRANTED);

        mvc.perform(post("/reminders/{id}/send", EVENT).with(user(me())).with(csrf()))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("https://wa.me/40700000001")));
        verify(messages).save(any(MessageEntity.class));
    }

    @Test
    void anotherBusinessAppointmentLooksLikeItDoesNotExist() throws Exception {
        appointmentOf(OTHER_BUSINESS, ConsentStatus.GRANTED);

        mvc.perform(post("/reminders/{id}/send", EVENT).with(user(me())).with(csrf()))
                .andExpect(status().isNotFound());
        verify(messages, never()).save(any(MessageEntity.class));
    }

    @Test
    void aCustomerWithoutConsentIsNeverMessaged() throws Exception {
        appointmentOf(MY_BUSINESS, ConsentStatus.OPTED_OUT);

        mvc.perform(post("/reminders/{id}/send", EVENT).with(user(me())).with(csrf()))
                .andExpect(status().isConflict());
        verify(messages, never()).save(any(MessageEntity.class));
    }

    @Test
    void aCancelledAppointmentIsNeverMessaged() throws Exception {
        EventEntity event = appointmentOf(MY_BUSINESS, ConsentStatus.GRANTED);
        when(event.getStatus()).thenReturn(EventStatus.CANCELLED);

        mvc.perform(post("/reminders/{id}/send", EVENT).with(user(me())).with(csrf()))
                .andExpect(status().isConflict());
        verify(messages, never()).save(any(MessageEntity.class));
    }

    @Test
    void youCanConfirmAndCancelYourOwnAppointments() throws Exception {
        EventEntity event = appointmentOf(MY_BUSINESS, ConsentStatus.GRANTED);

        mvc.perform(post("/appointments/{id}/confirm", EVENT).with(user(me())).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/appointments/{id}/cancel", EVENT).with(user(me())).with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(event).confirm();
        verify(event).cancel();
    }

    @Test
    void youCannotConfirmOrCancelAnotherBusinessAppointment() throws Exception {
        EventEntity event = appointmentOf(OTHER_BUSINESS, ConsentStatus.GRANTED);

        mvc.perform(post("/appointments/{id}/confirm", EVENT).with(user(me())).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/appointments/{id}/cancel", EVENT).with(user(me())).with(csrf()))
                .andExpect(status().isNotFound());

        verify(event, never()).confirm();
        verify(event, never()).cancel();
    }

    private void aBusinessExists() {
        BusinessEntity business = mock(BusinessEntity.class);
        when(business.getTimezone()).thenReturn("Europe/Bucharest");
        when(businesses.findById(MY_BUSINESS)).thenReturn(Optional.of(business));
    }

    @Test
    void aManualAppointmentIsSavedLikeAnImportedOne() throws Exception {
        aBusinessExists();

        mvc.perform(post("/appointments").with(user(me())).with(csrf())
                        .param("name", "Test Customer")
                        .param("phone", "0700 000 001")
                        .param("startsAt", "2026-11-24T10:00")
                        .param("consent", "da"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/?added"));
        verify(importer, times(1)).importRows(any(), any());
    }

    @Test
    void aManualAppointmentWithABadPhoneIsRejectedAndNothingIsSaved() throws Exception {
        aBusinessExists();

        mvc.perform(post("/appointments").with(user(me())).with(csrf())
                        .param("name", "Test Customer")
                        .param("phone", "12")
                        .param("startsAt", "2026-11-24T10:00"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Număr de telefon invalid")));
        verify(importer, never()).importRows(any(), any());
    }
}
