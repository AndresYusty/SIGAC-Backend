package com.universidad.sigac.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class FechaUtil {

    private static final Locale ES = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ES);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", ES);
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm:ss", ES);

    private FechaUtil() {
    }

    public static String fechaLarga(LocalDate d) {
        return d == null ? "" : d.format(FECHA_LARGA);
    }

    public static String hora(LocalDateTime d) {
        return d == null ? "" : d.format(HORA);
    }

    public static String fechaHoraLarga(Instant i, ZoneId zona) {
        return i == null ? "" : FECHA_HORA.format(i.atZone(zona));
    }
}
