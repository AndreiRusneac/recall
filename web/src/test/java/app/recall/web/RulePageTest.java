package app.recall.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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

import app.recall.core.RuleKind;
import app.recall.core.Vertical;
import app.recall.persistence.AppUserRepository;
import app.recall.persistence.BusinessEntity;
import app.recall.persistence.BusinessRepository;
import app.recall.persistence.RuleEntity;
import app.recall.persistence.RuleRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@Import({SecurityConfig.class, LoginThrottles.class, RuleController.class})
class RulePageTest {

    private static final UUID MY_BUSINESS = UUID.randomUUID();

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private BusinessRepository businesses;
    @MockitoBean
    private RuleRepository rules;
    @MockitoBean
    private AppUserRepository users;
    @MockitoBean
    private Clock clock;

    @BeforeEach
    void aBusinessWithoutItsOwnRule() {
        BusinessEntity business = mock(BusinessEntity.class);
        when(business.getId()).thenReturn(MY_BUSINESS);
        when(business.getName()).thenReturn("Test Clinic");
        when(business.getVertical()).thenReturn(Vertical.CLINIC);
        when(businesses.findById(MY_BUSINESS)).thenReturn(Optional.of(business));
        // A mock skips the real default method; make it fall back like a business with no rule.
        when(rules.reminderRuleFor(any(), any())).thenAnswer(call -> call.getArgument(1));
        when(rules.findUsable(MY_BUSINESS, RuleKind.REMINDER)).thenReturn(List.of());
    }

    private static RecallUser me() {
        return new RecallUser("owner@example.test", "hash", MY_BUSINESS);
    }

    @Test
    void theRulesPageAsksYouToLogInFirst() throws Exception {
        mvc.perform(get("/rules"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/login"));
    }

    @Test
    void showsTheDefaultWordingWithAPreview() throws Exception {
        mvc.perform(get("/rules").with(user(me())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Setări mesaj")))
                .andExpect(content().string(containsString("Maria Ionescu")))
                .andExpect(content().string(containsString("Test Clinic")))
                .andExpect(content().string(containsString("mesajul implicit")));
    }

    @Test
    void savingAGoodRuleReplacesTheOldOne() throws Exception {
        mvc.perform(post("/rules").with(user(me())).with(csrf())
                        .param("leadHours", "3")
                        .param("template", "Bună ziua, {name}! {business}, {date}, {time}."))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/rules?saved"));

        verify(rules).deleteByBusinessIdAndKind(MY_BUSINESS, RuleKind.REMINDER);
        ArgumentCaptor<RuleEntity> saved = ArgumentCaptor.forClass(RuleEntity.class);
        verify(rules).save(saved.capture());
        org.junit.jupiter.api.Assertions.assertEquals(180, saved.getValue().getLeadTimeMinutes());
        org.junit.jupiter.api.Assertions.assertTrue(saved.getValue().isEnabled());
    }

    @Test
    void aMisspelledPlaceholderIsRejectedAndNothingIsSaved() throws Exception {
        mvc.perform(post("/rules").with(user(me())).with(csrf())
                        .param("leadHours", "3")
                        .param("template", "Bună ziua, {nume}!"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("nu este un cuvânt cheie valid")))
                // What the owner typed stays in the form, so nothing has to be retyped.
                .andExpect(content().string(containsString("Bună ziua, {nume}!")));

        verify(rules, never()).deleteByBusinessIdAndKind(any(), any());
        verify(rules, never()).save(any());
    }

    @Test
    void anUnreadableOrOutOfRangeNumberOfHoursIsRejected() throws Exception {
        for (String hours : List.of("abc", "0", "9999", "")) {
            mvc.perform(post("/rules").with(user(me())).with(csrf())
                            .param("leadHours", hours)
                            .param("template", "Hi {name}"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Numărul de ore trebuie")));
        }
        verify(rules, never()).save(any());
    }

    @Test
    void resettingRemovesTheRuleSoTheDefaultsApplyAgain() throws Exception {
        mvc.perform(post("/rules/reset").with(user(me())).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/rules?reset"));

        verify(rules).deleteByBusinessIdAndKind(eq(MY_BUSINESS), eq(RuleKind.REMINDER));
    }

    @Test
    void changingRulesWithoutTheCsrfTokenIsRefused() throws Exception {
        mvc.perform(post("/rules").with(user(me()))
                        .param("leadHours", "3")
                        .param("template", "Hi {name}"))
                .andExpect(status().isForbidden());
        verify(rules, never()).save(any());
    }
}
