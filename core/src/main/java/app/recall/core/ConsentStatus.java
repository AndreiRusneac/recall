package app.recall.core;

/** Whether a customer agreed to receive messages. Only GRANTED customers are ever messaged. */
public enum ConsentStatus {
    UNKNOWN,
    GRANTED,
    OPTED_OUT
}
