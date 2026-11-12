package app.recall.core;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Builds a "click to chat" link. Opening it starts WhatsApp with the number and the text
 * already filled in, so a person only has to press send. This is how the pilot sends
 * messages without paying for the WhatsApp API.
 */
public final class WhatsAppLink {

    private WhatsAppLink() {
    }

    public static String to(String internationalPhone, String text) {
        String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
        return "https://wa.me/" + internationalPhone + "?text=" + encoded;
    }
}
