package com.university.sigac.core.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DateUtil {

    private static final Locale ES = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ES);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", ES);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm:ss", ES);

    private DateUtil() {
    }

    public static String longDate(LocalDate d) {
        return d == null ? "" : d.format(LONG_DATE);
    }

    public static String time(LocalDateTime d) {
        return d == null ? "" : d.format(TIME);
    }

    public static String longDateTime(Instant i, ZoneId zone) {
        return i == null ? "" : DATE_TIME.format(i.atZone(zone));
    }
}
