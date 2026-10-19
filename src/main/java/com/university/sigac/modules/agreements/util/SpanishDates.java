package com.university.sigac.modules.agreements.util;

import java.time.LocalDate;

/** Dates written the way the agreements write them: "veintisiete (27) días del mes de enero de dos mil veintiséis (2026)". */
public final class SpanishDates {

    private static final String[] MONTHS = {"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
            "agosto", "septiembre", "octubre", "noviembre", "diciembre"};
    private static final String[] UNITS = {"", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho",
            "nueve", "diez", "once", "doce", "trece", "catorce", "quince", "dieciséis", "diecisiete", "dieciocho",
            "diecinueve", "veinte", "veintiuno", "veintidós", "veintitrés", "veinticuatro", "veinticinco",
            "veintiséis", "veintisiete", "veintiocho", "veintinueve"};
    private static final String[] TENS = {"", "", "", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta",
            "ochenta", "noventa"};

    private SpanishDates() {
    }

    public static String month(LocalDate date) {
        return MONTHS[date.getMonthValue() - 1];
    }

    /** "(27 DE ENERO)" under the title. */
    public static String shortLine(LocalDate date) {
        return "(" + date.getDayOfMonth() + " DE " + month(date).toUpperCase() + ")";
    }

    /** Closing sentence: "Dado en Neiva, a los veintisiete (27) días del mes de enero de dos mil veintiséis (2026)." */
    public static String issuedOn(String city, LocalDate date) {
        int day = date.getDayOfMonth();
        String dayWords = day == 21 ? "veintiún" : words(day).replaceAll("uno$", "un");
        String days = day == 1 ? "al primer (1) día" : "a los " + dayWords + " (" + day + ") días";
        return "Dado en " + city + ", " + days + " del mes de " + month(date) + " de " + words(date.getYear())
                + " (" + date.getYear() + ").";
    }

    /** Cardinal number in Spanish, 0 to 9999. */
    public static String words(int n) {
        if (n == 0) {
            return "cero";
        }
        StringBuilder sb = new StringBuilder();
        int thousands = n / 1000;
        if (thousands > 0) {
            sb.append(thousands == 1 ? "mil" : words(thousands) + " mil");
        }
        int hundreds = n % 1000 / 100;
        int rest = n % 100;
        if (hundreds > 0) {
            append(sb, hundreds == 1 ? (rest == 0 ? "cien" : "ciento") : hundredsWord(hundreds));
        }
        if (rest > 0) {
            append(sb, rest < 30 ? UNITS[rest] : TENS[rest / 10] + (rest % 10 == 0 ? "" : " y " + UNITS[rest % 10]));
        }
        return sb.toString();
    }

    private static String hundredsWord(int h) {
        return switch (h) {
            case 5 -> "quinientos";
            case 7 -> "setecientos";
            case 9 -> "novecientos";
            default -> UNITS[h] + "cientos";
        };
    }

    private static void append(StringBuilder sb, String word) {
        if (!sb.isEmpty()) {
            sb.append(' ');
        }
        sb.append(word);
    }
}
