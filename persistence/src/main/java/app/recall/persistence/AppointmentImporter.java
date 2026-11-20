package app.recall.persistence;

import app.recall.core.AppointmentCsv;
import app.recall.core.ConsentStatus;
import app.recall.core.EventKind;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saves the rows of a parsed CSV file for one business. It is safe to run the same file twice:
 * customers are matched by phone number and an appointment that already exists is skipped.
 * All rows are saved in one transaction, so a failure leaves nothing half imported.
 */
@Component
public class AppointmentImporter {

    private final CustomerRepository customers;
    private final EventRepository events;

    public AppointmentImporter(CustomerRepository customers, EventRepository events) {
        this.customers = customers;
        this.events = events;
    }

    public record Summary(int customersCreated, int appointmentsCreated, int appointmentsSkipped) {
    }

    @Transactional
    public Summary importRows(BusinessEntity business, List<AppointmentCsv.Row> rows) {
        int customersCreated = 0;
        int appointmentsCreated = 0;
        int appointmentsSkipped = 0;

        for (AppointmentCsv.Row row : rows) {
            CustomerEntity customer = customers.findByBusinessIdAndPhone(business.getId(), row.phone()).orElse(null);
            if (customer == null) {
                customer = customers.save(new CustomerEntity(business, row.name(), row.phone(), row.consent()));
                customersCreated++;
            } else {
                // The rule for what a file may change lives in core, next to its tests.
                customer.changeConsent(ConsentStatus.afterImport(customer.getConsent(), row.consent()));
            }

            if (events.existsByCustomerIdAndKindAndStartsAt(customer.getId(), EventKind.APPOINTMENT, row.startsAt())) {
                appointmentsSkipped++;
            } else {
                events.save(new EventEntity(business, customer, EventKind.APPOINTMENT, row.startsAt()));
                appointmentsCreated++;
            }
        }
        return new Summary(customersCreated, appointmentsCreated, appointmentsSkipped);
    }
}
