package com.universidad.sigac.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Utilidades criptográficas: digest SHA-256 (64 caracteres hexadecimales). */
public final class HashUtil {

    private HashUtil() {
    }

    public static String sha256Hex(byte[] data) {
        return HexFormat.of().formatHex(digest().digest(data));
    }

    public static String sha256Hex(String text) {
        return sha256Hex(text.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256Hex(Path file) {
        MessageDigest md = digest();
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                md.update(buf, 0, n);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible leer el archivo para calcular el hash", e);
        }
        return HexFormat.of().formatHex(md.digest());
    }

    /** Comparación en tiempo constante de dos hashes hexadecimales. */
    public static boolean iguales(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.toLowerCase().getBytes(StandardCharsets.UTF_8),
                b.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en la JVM", e);
        }
    }
}
