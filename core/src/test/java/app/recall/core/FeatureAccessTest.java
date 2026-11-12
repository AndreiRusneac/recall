package app.recall.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FeatureAccessTest {

    @Test
    void onlyTheBookingPlanGetsOnlineBooking() {
        assertFalse(FeatureAccess.allows(Plan.REMINDERS, Feature.ONLINE_BOOKING));
        assertTrue(FeatureAccess.allows(Plan.BOOKING, Feature.ONLINE_BOOKING));
    }
}
