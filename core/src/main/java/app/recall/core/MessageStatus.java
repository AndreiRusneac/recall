package app.recall.core;

/**
 * What we know happened to a message. Stored as text in the message table.
 * Each status claims only what Recall can actually observe.
 */
public enum MessageStatus {

    /**
     * WhatsApp was opened with the text filled in. Whether the person then pressed send
     * happens on their phone, and Recall cannot see it (decision 002).
     */
    OPENED,

    /** Recall handed the message to a sender and the sender accepted it. */
    SENT
}
