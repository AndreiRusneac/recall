package app.recall.core;

/** How a message left Recall. Stored as text in the message table. */
public enum MessageChannel {

    /** Tap-to-send: the owner opened a wa.me link and WhatsApp on their own device did the rest. */
    WHATSAPP_LINK,

    /** Sent by Recall itself through a MessageSender, without a person in the loop. */
    WHATSAPP_API
}