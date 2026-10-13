package com.university.sigac.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Cryptographic utilities: SHA-256 digest (64 hexadecimal characters). */
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
            throw new IllegalStateException("Could not read the file to compute its hash", e);
        }
        return HexFormat.of().formatHex(md.digest());
    }

    /** Constant-time comparison of two hexadecimal hashes. */
    public static boolean matches(String a, String b) {
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
            throw new IllegalStateException("SHA-256 is not available in the JVM", e);
        }
    }
}
