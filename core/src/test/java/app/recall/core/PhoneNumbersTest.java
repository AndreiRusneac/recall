package app.recall.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PhoneNumbersTest {

    @Test
    void addsTheCountryCodeToLocalNumbers() {
        assertEquals("40700000001", PhoneNumbers.toInternational("0700 000 001", "40"));
    }

    @Test
    void keepsNumbersThatAlreadyHaveACountryCode() {
        assertEquals("40700000001", PhoneNumbers.toInternational("+40 700 000 001", "40"));
        assertEquals("40700000001", PhoneNumbers.toInternational("0040 700 000 001", "40"));
    }

    @Test
    void buildsAClickToChatLink() {
        assertEquals(
                "https://wa.me/40700000001?text=See%20you%20at%2010%3A00",
                WhatsAppLink.to("40700000001", "See you at 10:00"));
    }
}
