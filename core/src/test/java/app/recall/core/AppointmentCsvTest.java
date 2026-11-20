package app.recall.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class AppointmentCsvTest {

    private static final ZoneId BUCHAREST = ZoneId.of("Europe/Bucharest");

    private static AppointmentCsv.Result parse(String text) {
        return AppointmentCsv.parse(text, BUCHAREST);
    }

    @Test
    void readsAValidRow() {
        var result = parse("name,phone,starts_at,consent\nTest Customer,0700 000 001,2026-11-24 10:00,yes\n");

        assertTrue(result.errors().isEmpty());
        var row = result.rows().get(0);
        assertEquals("Test Customer", row.name());
        assertEquals("40700000001", row.phone());
        assertEquals(ConsentStatus.GRANTED, row.consent());
        // 10:00 in Bucharest is UTC+2 in November.
        assertEquals(Instant.parse("2026-11-24T08:00:00Z"), row.startsAt());
        assertEquals(2, row.line());
    }

    @Test
    void acceptsSemicolonsRomanianHeadersAndRomanianDates() {
        var result = parse("Nume;Telefon;Programare;Acord\nTest Customer;+40 700 000 001;24.11.2026 10:00;da\n");

        assertTrue(result.errors().isEmpty());
        assertEquals(Instant.parse("2026-11-24T08:00:00Z"), result.rows().get(0).startsAt());
        assertEquals(ConsentStatus.GRANTED, result.rows().get(0).consent());
    }

    @Test
    void ignoresTheByteOrderMarkExcelAdds() {
        var result = parse("﻿name,phone,starts_at\nTest Customer,0700000001,2026-11-24 10:00\n");

        assertTrue(result.errors().isEmpty());
        assertEquals(1, result.rows().size());
    }

    @Test
    void keepsCommasAndQuotesInsideQuotedFields() {
        var result = parse("name,phone,starts_at\n\"Customer, Test \"\"Jr\"\"\",0700000001,2026-11-24 10:00\n");

        assertEquals("Customer, Test \"Jr\"", result.rows().get(0).name());
    }

    @Test
    void emptyConsentColumnMeansUnknown() {
        var result = parse("name,phone,starts_at\nTest Customer,0700000001,2026-11-24 10:00\n");

        assertEquals(ConsentStatus.UNKNOWN, result.rows().get(0).consent());
    }

    @Test
    void neverGuessesAnUnclearConsentValue() {
        var result = parse("name,phone,starts_at,consent\nTest Customer,0700000001,2026-11-24 10:00,maybe\n");

        assertTrue(result.rows().isEmpty());
        assertEquals(AppointmentCsv.Problem.BAD_CONSENT, result.errors().get(0).problem());
        assertEquals("maybe", result.errors().get(0).detail());
    }

    @Test
    void reportsBadRowsWithTheirLineAndKeepsTheGoodOnes() {
        var result = parse("""
                name,phone,starts_at,consent
                Good One,0700000001,2026-11-24 10:00,yes
                ,0700000002,2026-11-24 11:00,yes
                Bad Phone,12,2026-11-24 11:00,yes
                Bad Date,0700000003,tomorrow,yes
                Good Two,0700000004,2026-11-25 09:30,no
                """);

        assertEquals(2, result.rows().size());
        assertEquals(3, result.errors().size());
        assertEquals(3, result.errors().get(0).line());
        assertEquals(AppointmentCsv.Problem.MISSING_NAME, result.errors().get(0).problem());
        assertEquals(AppointmentCsv.Problem.BAD_PHONE, result.errors().get(1).problem());
        assertEquals(AppointmentCsv.Problem.BAD_DATE, result.errors().get(2).problem());
        assertEquals(ConsentStatus.OPTED_OUT, result.rows().get(1).consent());
    }

    @Test
    void skipsBlankLines() {
        var result = parse("name,phone,starts_at\n\nTest Customer,0700000001,2026-11-24 10:00\n\n");

        assertTrue(result.errors().isEmpty());
        assertEquals(1, result.rows().size());
    }

    @Test
    void reportsAMissingRequiredColumnOnce() {
        var result = parse("name,starts_at\nTest Customer,2026-11-24 10:00\n");

        assertTrue(result.rows().isEmpty());
        assertEquals(1, result.errors().size());
        assertEquals(AppointmentCsv.Problem.MISSING_COLUMN, result.errors().get(0).problem());
        assertEquals("phone", result.errors().get(0).detail());
    }

    @Test
    void anEmptyFileIsAnError() {
        assertEquals(AppointmentCsv.Problem.MISSING_COLUMN, parse("").errors().get(0).problem());
    }

    @Test
    void stopsAtTheRowLimit() {
        StringBuilder text = new StringBuilder("name,phone,starts_at\n");
        for (int i = 0; i < AppointmentCsv.MAX_ROWS + 5; i++) {
            text.append("Test Customer,0700000001,2026-11-24 10:00\n");
        }
        var result = parse(text.toString());

        assertEquals(AppointmentCsv.MAX_ROWS, result.rows().size());
        assertEquals(AppointmentCsv.Problem.TOO_MANY_ROWS, result.errors().get(0).problem());
    }

    @Test
    void anImportNeverUndoesAnOptOut() {
        assertEquals(ConsentStatus.OPTED_OUT, ConsentStatus.afterImport(ConsentStatus.OPTED_OUT, ConsentStatus.GRANTED));
        assertEquals(ConsentStatus.OPTED_OUT, ConsentStatus.afterImport(ConsentStatus.OPTED_OUT, ConsentStatus.UNKNOWN));
    }

    @Test
    void anImportCanGrantUnknownAndCanOptOut() {
        assertEquals(ConsentStatus.GRANTED, ConsentStatus.afterImport(ConsentStatus.UNKNOWN, ConsentStatus.GRANTED));
        assertEquals(ConsentStatus.OPTED_OUT, ConsentStatus.afterImport(ConsentStatus.GRANTED, ConsentStatus.OPTED_OUT));
    }

    @Test
    void anEmptyAnswerKeepsWhatWeKnow() {
        assertEquals(ConsentStatus.GRANTED, ConsentStatus.afterImport(ConsentStatus.GRANTED, ConsentStatus.UNKNOWN));
        assertEquals(ConsentStatus.UNKNOWN, ConsentStatus.afterImport(ConsentStatus.UNKNOWN, ConsentStatus.UNKNOWN));
    }
}
