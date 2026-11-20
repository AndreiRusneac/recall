package app.recall.core;

/** Whether a customer agreed to receive messages. Only GRANTED customers are ever messaged. */
public enum ConsentStatus {
    UNKNOWN,
    GRANTED,
    OPTED_OUT;

    /**
     * What a customer's consent becomes when a file we import says {@code imported}.
     * An opt-out is never undone by a file: only the customer can take it back, so a later
     * import that says "yes" for someone who opted out is ignored. A file that says "no"
     * always wins, and an empty answer (UNKNOWN) never changes what we already know.
     */
    public static ConsentStatus afterImport(ConsentStatus existing, ConsentStatus imported) {
        if (existing == OPTED_OUT || imported == OPTED_OUT) {
            return OPTED_OUT;
        }
        return imported == UNKNOWN ? existing : imported;
    }
}
