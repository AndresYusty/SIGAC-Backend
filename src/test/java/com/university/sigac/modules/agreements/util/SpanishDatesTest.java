package com.university.sigac.modules.agreements.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SpanishDatesTest {

    @Test
    void writesTheClosingSentenceLikeTheModels() {
        assertEquals("Dado en Neiva, a los veintisiete (27) días del mes de enero de dos mil veintiséis (2026).",
                SpanishDates.issuedOn("Neiva", LocalDate.of(2026, 1, 27)));
        assertEquals("Dado en Neiva, al primer (1) día del mes de marzo de dos mil veintiséis (2026).",
                SpanishDates.issuedOn("Neiva", LocalDate.of(2026, 3, 1)));
        assertEquals("Dado en Pitalito, a los treinta y un (31) días del mes de agosto de dos mil treinta (2030).",
                SpanishDates.issuedOn("Pitalito", LocalDate.of(2030, 8, 31)));
        assertEquals("Dado en Neiva, a los veintiún (21) días del mes de mayo de dos mil veintiuno (2021).",
                SpanishDates.issuedOn("Neiva", LocalDate.of(2021, 5, 21)));
    }

    @Test
    void writesTheShortDateUnderTheTitle() {
        assertEquals("(27 DE ENERO)", SpanishDates.shortLine(LocalDate.of(2026, 1, 27)));
    }

    @Test
    void writesNumbersInWords() {
        assertEquals("ciento quince", SpanishDates.words(115));
        assertEquals("quinientos cuarenta y dos", SpanishDates.words(542));
        assertEquals("mil novecientos noventa y nueve", SpanishDates.words(1999));
    }
}
