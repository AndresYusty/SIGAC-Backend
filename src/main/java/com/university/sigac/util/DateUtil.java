package com.university.sigac.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DateUtil {

    private static final Locale EN = Locale.ENGLISH;
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("MMMM d, yyyy", EN);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", EN);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("MMMM d, yyyy, HH:mm:ss", EN);

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
