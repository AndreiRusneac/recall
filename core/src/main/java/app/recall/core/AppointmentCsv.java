package app.recall.core;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reads a clinic's appointment list from CSV text. Pure logic: text in, rows and errors out.
 *
 * <p>Expected columns (header row required, order does not matter, Romanian names also work):
 * <ul>
 *   <li>{@code name} (or nume): the customer's name</li>
 *   <li>{@code phone} (or telefon): any common way of writing a number</li>
 *   <li>{@code starts_at} (or data, programare): {@code 2026-11-24 10:00} or {@code 24.11.2026 10:00}</li>
 *   <li>{@code consent} (or acord), optional: yes/da means GRANTED, no/nu means OPTED_OUT, empty means UNKNOWN</li>
 * </ul>
 * Commas, semicolons (what Excel produces in Romania) and tabs are all accepted as separators.
 * A bad row never stops the others: it is reported in {@link Result#errors()} and skipped.
 */
public final class AppointmentCsv {

    /** A safety limit so one upload cannot hold the server busy. */
    public static final int MAX_ROWS = 2000;

    private static final String DEFAULT_COUNTRY_CODE = "40";

    private static final Map<String, String> HEADER_ALIASES = Map.ofEntries(
            Map.entry("name", "name"),
            Map.entry("nume", "name"),
            Map.entry("customer", "name"),
            Map.entry("phone", "phone"),
            Map.entry("telefon", "phone"),
            Map.entry("tel", "phone"),
            Map.entry("starts_at", "starts_at"),
            Map.entry("data", "starts_at"),
            Map.entry("date", "starts_at"),
            Map.entry("datetime", "starts_at"),
            Map.entry("programare", "starts_at"),
            Map.entry("appointment", "starts_at"),
            Map.entry("consent", "consent"),
            Map.entry("acord", "consent"));

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

    private static final Set<String> YES = Set.of("yes", "y", "da", "granted", "true", "1");
    private static final Set<String> NO = Set.of("no", "n", "nu", "opted_out", "false", "0");

    private AppointmentCsv() {
    }

    /** One valid appointment from the file. {@code phone} is already in international digits. */
    public record Row(int line, String name, String phone, Instant startsAt, ConsentStatus consent) {
    }

    public enum Problem {
        /** A required column is missing from the header row. {@code detail} names it. */
        MISSING_COLUMN,
        MISSING_NAME,
        /** {@code detail} is the value as written in the file. */
        BAD_PHONE,
        BAD_DATE,
        BAD_CONSENT,
        /** The file has more than {@link #MAX_ROWS} rows. */
        TOO_MANY_ROWS
    }

    public record RowError(int line, Problem problem, String detail) {
    }

    public record Result(List<Row> rows, List<RowError> errors) {
    }

    /** Parses the text of a CSV file. Dates are read as local time in {@code zone}. */
    public static Result parse(String text, ZoneId zone) {
        List<Row> rows = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();

        String cleaned = text.startsWith("﻿") ? text.substring(1) : text;
        char delimiter = detectDelimiter(cleaned);
        List<List<String>> records = splitRecords(cleaned, delimiter);
        if (records.isEmpty()) {
            errors.add(new RowError(1, Problem.MISSING_COLUMN, "name"));
            return new Result(rows, errors);
        }

        Map<String, Integer> columns = new HashMap<>();
        List<String> header = records.get(0);
        for (int i = 0; i < header.size(); i++) {
            String canonical = HEADER_ALIASES.get(header.get(i).trim().toLowerCase(Locale.ROOT));
            if (canonical != null) {
                columns.putIfAbsent(canonical, i);
            }
        }
        for (String required : List.of("name", "phone", "starts_at")) {
            if (!columns.containsKey(required)) {
                errors.add(new RowError(1, Problem.MISSING_COLUMN, required));
            }
        }
        if (!errors.isEmpty()) {
            return new Result(rows, errors);
        }

        int dataRows = 0;
        for (int index = 1; index < records.size(); index++) {
            List<String> fields = records.get(index);
            int line = index + 1;
            if (fields.stream().allMatch(String::isBlank)) {
                continue;
            }
            if (++dataRows > MAX_ROWS) {
                errors.add(new RowError(line, Problem.TOO_MANY_ROWS, String.valueOf(MAX_ROWS)));
                break;
            }

            String name = field(fields, columns.get("name"));
            if (name.isEmpty()) {
                errors.add(new RowError(line, Problem.MISSING_NAME, ""));
                continue;
            }
            String rawPhone = field(fields, columns.get("phone"));
            String phone = PhoneNumbers.toInternational(rawPhone, DEFAULT_COUNTRY_CODE);
            if (phone.length() < 10 || phone.length() > 15) {
                errors.add(new RowError(line, Problem.BAD_PHONE, rawPhone));
                continue;
            }
            String rawDate = field(fields, columns.get("starts_at"));
            Instant startsAt = parseDate(rawDate, zone);
            if (startsAt == null) {
                errors.add(new RowError(line, Problem.BAD_DATE, rawDate));
                continue;
            }
            String rawConsent = columns.containsKey("consent") ? field(fields, columns.get("consent")) : "";
            ConsentStatus consent = parseConsent(rawConsent);
            if (consent == null) {
                errors.add(new RowError(line, Problem.BAD_CONSENT, rawConsent));
                continue;
            }
            rows.add(new Row(line, name, phone, startsAt, consent));
        }
        return new Result(rows, errors);
    }

    private static String field(List<String> fields, int index) {
        return index < fields.size() ? fields.get(index).trim() : "";
    }

    private static Instant parseDate(String value, ZoneId zone) {
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDateTime.parse(value, format).atZone(zone).toInstant();
            } catch (DateTimeParseException ignored) {
                // Try the next format.
            }
        }
        return null;
    }

    /** Empty means UNKNOWN. Returns null for a value we do not understand, so it is never guessed. */
    private static ConsentStatus parseConsent(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return ConsentStatus.UNKNOWN;
        }
        if (YES.contains(normalized)) {
            return ConsentStatus.GRANTED;
        }
        if (NO.contains(normalized)) {
            return ConsentStatus.OPTED_OUT;
        }
        return null;
    }

    /** Picks the separator that appears most often in the header line. */
    private static char detectDelimiter(String text) {
        int end = text.indexOf('\n');
        String header = end < 0 ? text : text.substring(0, end);
        long semicolons = header.chars().filter(c -> c == ';').count();
        long commas = header.chars().filter(c -> c == ',').count();
        long tabs = header.chars().filter(c -> c == '\t').count();
        if (semicolons > commas && semicolons >= tabs) {
            return ';';
        }
        if (tabs > commas) {
            return '\t';
        }
        return ',';
    }

    /** Splits text into records and fields. Handles quoted fields, doubled quotes and line breaks inside quotes. */
    private static List<List<String>> splitRecords(String text, char delimiter) {
        List<List<String>> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == delimiter) {
                fields.add(current.toString());
                current.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                fields.add(current.toString());
                current.setLength(0);
                records.add(fields);
                fields = new ArrayList<>();
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0 || !fields.isEmpty()) {
            fields.add(current.toString());
            records.add(fields);
        }
        return records;
    }
}
