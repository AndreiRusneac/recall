package app.recall.core;

/**
 * The port for sending messages. The core only knows this interface;
 * the messaging module provides the implementations (a logging fake today, WhatsApp later).
 */
public interface MessageSender {

    void send(OutboundMessage message);
}
