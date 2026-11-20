package app.recall.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import app.recall.core.AppointmentCsv;
import app.recall.core.ConsentStatus;
import app.recall.core.EventKind;
import app.recall.core.Plan;
import app.recall.core.Vertical;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@PostgresJpaTest
@Import(AppointmentImporter.class)
class AppointmentImporterTest {

    private static final Instant NINE = Instant.parse("2026-11-24T09:00:00Z");
    private static final Instant TEN = Instant.parse("2026-11-24T10:00:00Z");

    @Autowired
    private AppointmentImporter importer;

    @Autowired
    private CustomerRepository customers;

    @Autowired
    private EventRepository events;

    @Autowired
    private TestEntityManager entityManager;

    private BusinessEntity clinic;

    @BeforeEach
    void setUp() {
        clinic = entityManager.persist(new BusinessEntity("Test Clinic", Vertical.CLINIC, Plan.REMINDERS, "Europe/Bucharest"));
    }

    private static AppointmentCsv.Row row(String phone, Instant startsAt, ConsentStatus consent) {
        return new AppointmentCsv.Row(2, "Test Customer", phone, startsAt, consent);
    }

    @Test
    void createsTheCustomerAndTheAppointment() {
        var summary = importer.importRows(clinic, List.of(row("40700000001", TEN, ConsentStatus.GRANTED)));

        assertThat(summary).isEqualTo(new AppointmentImporter.Summary(1, 1, 0));
        CustomerEntity customer = customers.findByBusinessIdAndPhone(clinic.getId(), "40700000001").orElseThrow();
        assertThat(customer.getConsent()).isEqualTo(ConsentStatus.GRANTED);
        assertThat(events.existsByCustomerIdAndKindAndStartsAt(customer.getId(), EventKind.APPOINTMENT, TEN)).isTrue();
    }

    @Test
    void importingTheSameFileTwiceChangesNothingTheSecondTime() {
        List<AppointmentCsv.Row> rows = List.of(row("40700000001", TEN, ConsentStatus.GRANTED));
        importer.importRows(clinic, rows);

        var second = importer.importRows(clinic, rows);

        assertThat(second).isEqualTo(new AppointmentImporter.Summary(0, 0, 1));
        assertThat(customers.count()).isEqualTo(1);
        assertThat(events.count()).isEqualTo(1);
    }

    @Test
    void aReturningCustomerGetsASecondAppointmentButNotASecondRecord() {
        var summary = importer.importRows(clinic, List.of(
                row("40700000001", TEN, ConsentStatus.GRANTED),
                row("40700000001", NINE, ConsentStatus.GRANTED)));

        assertThat(summary).isEqualTo(new AppointmentImporter.Summary(1, 2, 0));
    }

    @Test
    void aFileCannotUndoAnOptOut() {
        entityManager.persist(new CustomerEntity(clinic, "Test Customer", "40700000001", ConsentStatus.OPTED_OUT));
        entityManager.flush();

        importer.importRows(clinic, List.of(row("40700000001", TEN, ConsentStatus.GRANTED)));
        entityManager.flush();
        entityManager.clear();

        assertThat(customers.findByBusinessIdAndPhone(clinic.getId(), "40700000001").orElseThrow().getConsent())
                .isEqualTo(ConsentStatus.OPTED_OUT);
    }

    @Test
    void aFileCanGrantConsentThatWasUnknown() {
        entityManager.persist(new CustomerEntity(clinic, "Test Customer", "40700000001", ConsentStatus.UNKNOWN));
        entityManager.flush();

        importer.importRows(clinic, List.of(row("40700000001", TEN, ConsentStatus.GRANTED)));
        entityManager.flush();
        entityManager.clear();

        assertThat(customers.findByBusinessIdAndPhone(clinic.getId(), "40700000001").orElseThrow().getConsent())
                .isEqualTo(ConsentStatus.GRANTED);
    }

    @Test
    void theSamePhoneInAnotherBusinessIsAnotherCustomer() {
        BusinessEntity other = entityManager.persist(
                new BusinessEntity("Other Clinic", Vertical.CLINIC, Plan.REMINDERS, "Europe/Bucharest"));

        importer.importRows(clinic, List.of(row("40700000001", TEN, ConsentStatus.GRANTED)));
        var summary = importer.importRows(other, List.of(row("40700000001", TEN, ConsentStatus.GRANTED)));

        assertThat(summary.customersCreated()).isEqualTo(1);
        assertThat(customers.count()).isEqualTo(2);
    }
}
