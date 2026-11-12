package app.recall.web;

import app.recall.core.ConsentStatus;
import app.recall.core.EventKind;
import app.recall.core.EventStatus;
import app.recall.core.ReminderPlanner;
import app.recall.core.ReminderRule;
import app.recall.core.UpcomingAppointment;
import app.recall.persistence.BusinessEntity;
import app.recall.persistence.BusinessRepository;
import app.recall.persistence.EventRepository;
import app.recall.verticals.VerticalDefaults;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** The owner's home page: the next seven days of appointments and which reminders are due. */
@Controller
public class DashboardController {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEE dd.MM, HH:mm", Locale.ENGLISH);

    private final BusinessRepository businesses;
    private final EventRepository events;
    private final Clock clock;
    private final ReminderPlanner planner = new ReminderPlanner();

    public DashboardController(BusinessRepository businesses, EventRepository events, Clock clock) {
        this.businesses = businesses;
        this.events = events;
        this.clock = clock;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        // Temporary: there is no login yet, so we show the first business in the database.
        Optional<BusinessEntity> found = businesses.findFirstByOrderByCreatedAtAsc();
        if (found.isEmpty()) {
            return "empty";
        }
        BusinessEntity business = found.get();
        ZoneId zone = ZoneId.of(business.getTimezone());
        Instant now = clock.instant();
        ReminderRule rule = VerticalDefaults.reminderRule(business.getVertical());

        List<AppointmentRow> rows = events
                .findStartingBetween(
                        business.getId(),
                        EventKind.APPOINTMENT,
                        EventStatus.CANCELLED,
                        now,
                        now.plus(Duration.ofDays(7)))
                .stream()
                .map(event -> {
                    UpcomingAppointment appointment = event.toUpcoming();
                    return new AppointmentRow(
                            appointment.id(),
                            appointment.customerName(),
                            WHEN.format(appointment.startsAt().atZone(zone)),
                            planner.isDue(appointment, rule, now),
                            appointment.reminded(),
                            appointment.consent() == ConsentStatus.GRANTED,
                            planner.render(business.getName(), zone, rule, appointment).text());
                })
                .toList();

        model.addAttribute("businessName", business.getName());
        model.addAttribute("rows", rows);
        model.addAttribute("dueCount", rows.stream().filter(AppointmentRow::due).count());
        return "dashboard";
    }

    /** One line of the dashboard table, already formatted for display. */
    public record AppointmentRow(
            UUID id,
            String customerName,
            String when,
            boolean due,
            boolean reminded,
            boolean hasConsent,
            String text) {
    }
}
