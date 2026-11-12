package app.recall.core;

/** The single place that answers "may this business use that feature?". */
public final class FeatureAccess {

    private FeatureAccess() {
    }

    public static boolean allows(Plan plan, Feature feature) {
        return switch (feature) {
            case ONLINE_BOOKING -> plan == Plan.BOOKING;
        };
    }
}
