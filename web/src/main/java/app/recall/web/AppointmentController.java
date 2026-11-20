package app.recall.web;

import app.recall.core.AppointmentCsv;
import app.recall.persistence.AppointmentImporter;
import app.recall.persistence.BusinessEntity;
import app.recall.persistence.BusinessRepository;
import app.recall.persistence.EventEntity;
import app.recall.persistence.EventRepository;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/** Adding one appointment by hand, and recording that a customer confirmed or cancelled. */
@Controller
public class AppointmentController {

    private final BusinessRepository businesses;
    private final EventRepository events;
    private final AppointmentImporter importer;

    public AppointmentController(BusinessRepository businesses, EventRepository events, AppointmentImporter importer) {
        this.businesses = businesses;
        this.events = events;
        this.importer = importer;
    }

    @GetMapping("/appointments/new")
    public String form() {
        return "new-appointment";
    }

    /** Uses the same checks and the same saving as the CSV import, so both paths follow the same rules. */
    @PostMapping("/appointments")
    public String create(
            @AuthenticationPrincipal RecallUser user,
            @RequestParam String name,
            @RequestParam String phone,
            @RequestParam String startsAt,
            @RequestParam(defaultValue = "") String consent,
            Model model) {
        BusinessEntity business = businesses.findById(user.getBusinessId()).orElseThrow();
        AppointmentCsv.Result checked = AppointmentCsv.parseFields(
                1, name, phone, startsAt, consent, ZoneId.of(business.getTimezone()));

        if (!checked.errors().isEmpty()) {
            model.addAttribute("error", ImportController.describe(checked.errors().get(0)));
            model.addAttribute("name", name);
            model.addAttribute("phone", phone);
            model.addAttribute("startsAt", startsAt);
            model.addAttribute("consent", consent);
            return "new-appointment";
        }
        importer.importRows(business, checked.rows());
        return "redirect:/?added";
    }

    @PostMapping("/appointments/{id}/confirm")
    @Transactional
    public String confirm(@AuthenticationPrincipal RecallUser user, @PathVariable UUID id) {
        ownEvent(user, id).confirm();
        return "redirect:/";
    }

    @PostMapping("/appointments/{id}/cancel")
    @Transactional
    public String cancel(@AuthenticationPrincipal RecallUser user, @PathVariable UUID id) {
        ownEvent(user, id).cancel();
        return "redirect:/";
    }

    /** Someone else's appointment looks exactly like one that does not exist. */
    private EventEntity ownEvent(RecallUser user, UUID id) {
        return events.findById(id)
                .filter(event -> event.getBusiness().getId().equals(user.getBusinessId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
