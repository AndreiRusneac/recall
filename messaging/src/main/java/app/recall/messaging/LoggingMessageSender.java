package app.recall.messaging;

import app.recall.core.MessageSender;
import app.recall.core.OutboundMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The fake sender: it writes the message to the log and sends nothing.
 * Develop against this. The real WhatsApp adapter will be a second implementation
 * of MessageSender in this module, chosen by configuration.
 */
@Component
public class LoggingMessageSender implements MessageSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingMessageSender.class);

    @Override
    public void send(OutboundMessage message) {
        log.info("FAKE SEND to {}: {}", message.toPhone(), message.text());
    }
}
