package app.recall.core;

import java.util.Map;

/** Replaces {placeholders} in a template with values. */
public final class TemplateRenderer {

    private TemplateRenderer() {
    }

    public static String render(String template, Map<String, String> values) {
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
