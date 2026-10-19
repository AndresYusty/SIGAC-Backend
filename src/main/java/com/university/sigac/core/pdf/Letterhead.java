package com.university.sigac.core.pdf;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

/** Institutional page sheet stored with the templates. Referenced as {@code letterhead:usco}. */
public final class Letterhead {

    public static final String TOKEN = "letterhead:usco";

    private static final String DATA_URI = load();

    private Letterhead() {
    }

    /** Replaces the sheet token with an embedded PNG so the PDF engine can draw it. */
    public static String embed(String html) {
        if (html == null || !html.contains(TOKEN)) {
            return html;
        }
        return html.replace(TOKEN, DATA_URI);
    }

    private static String load() {
        try (InputStream in = Letterhead.class.getResourceAsStream("/sigac/templates/letterhead.png")) {
            if (in == null) {
                throw new IllegalStateException("Missing resource /sigac/templates/letterhead.png");
            }
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(in.readAllBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read the letterhead", e);
        }
    }
}
