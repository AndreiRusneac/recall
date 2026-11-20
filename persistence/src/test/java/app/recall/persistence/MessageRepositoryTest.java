package app.recall.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import app.recall.core.ConsentStatus;
import app.recall.core.EventKind;
import app.recall.core.MessageChannel;
import app.recall.core.MessageStatus;
import app.recall.core.Plan;
import app.recall.core.Vertical;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@PostgresJpaTest
class MessageRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-11-16T12:00:00Z");

    @Autowired
    private MessageRepository messages;

    @Autowired
    private TestEntityManager entityManager;

    private EventEntity appointment;

    @BeforeEach
    void setUp() {
        BusinessEntity clinic = entityManager.persist(
                new BusinessEntity("Test Clinic", Vertical.CLINIC, Plan.REMINDERS, "Europe/Bucharest"));
        CustomerEntity customer = entityManager.persist(
                new CustomerEntity(clinic, "Test Customer", "0700 000 001", ConsentStatus.GRANTED));
        appointment = entityManager.persist(
                new EventEntity(clinic, customer, EventKind.APPOINTMENT, NOW.plus(Duration.ofHours(20))));
    }

    private MessageEntity saveAndReload(MessageEntity message) {
        MessageEntity saved = messages.save(message);
        entityManager.flush();
        entityManager.clear();
        return messages.findById(saved.getId()).orElseThrow();
    }

    @Test
    void savesAReminderLinkedToItsAppointment() {
        MessageEntity reloaded = saveAndReload(new MessageEntity(
                appointment, MessageChannel.WHATSAPP_LINK, "Reminder text", MessageStatus.OPENED, NOW));

        assertThat(reloaded.getEvent().getId()).isEqualTo(appointment.getId());
        assertThat(reloaded.getBusiness().getId()).isEqualTo(appointment.getBusiness().getId());
        assertThat(reloaded.getCustomer().getId()).isEqualTo(appointment.getCustomer().getId());
        assertThat(reloaded.getChannel()).isEqualTo(MessageChannel.WHATSAPP_LINK);
        assertThat(reloaded.getStatus()).isEqualTo(MessageStatus.OPENED);
        assertThat(reloaded.getBody()).isEqualTo("Reminder text");
        assertThat(reloaded.getSentAt()).isEqualTo(NOW);
        assertThat(reloaded.getCreatedAt()).isNotNull();
    }

    @Test
    void storesChannelAndStatusAsReadableText() {
        MessageEntity saved = messages.save(new MessageEntity(
                appointment, MessageChannel.WHATSAPP_API, "Reminder text", MessageStatus.SENT, NOW));
        entityManager.flush();

        // Stats queries and anyone using psql read these columns directly, so they must hold the names.
        Object[] row = (Object[]) entityManager.getEntityManager()
                .createNativeQuery("select channel, status from message where id = :id")
                .setParameter("id", saved.getId())
                .getSingleResult();
        assertThat(row).containsExactly("WHATSAPP_API", "SENT");
    }

    @Test
    void keepsRomanianLettersInTheBody() {
        String body = "Bună ziua, vă reamintim programarea. Răspundeți cu 1 pentru confirmare.";

        MessageEntity reloaded = saveAndReload(new MessageEntity(
                appointment, MessageChannel.WHATSAPP_LINK, body, MessageStatus.OPENED, NOW));

        assertThat(reloaded.getBody()).isEqualTo(body);
    }
}
