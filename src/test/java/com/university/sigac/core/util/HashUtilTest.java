package com.university.sigac.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HashUtilTest {

    @Test
    void knownSha256OfAbc() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", HashUtil.sha256Hex("abc"));
    }

    @Test
    void fileHashMatchesBytesHash(@TempDir Path dir) throws Exception {
        byte[] data = "minutes content".getBytes(StandardCharsets.UTF_8);
        Path f = dir.resolve("minutes.pdf");
        Files.write(f, data);
        assertEquals(HashUtil.sha256Hex(data), HashUtil.sha256Hex(f));
    }

    @Test
    void anyChangeAltersTheHash() {
        assertFalse(HashUtil.matches(HashUtil.sha256Hex("minutes v1"), HashUtil.sha256Hex("minutes v1 ")));
        assertTrue(HashUtil.matches(HashUtil.sha256Hex("x"), HashUtil.sha256Hex("x").toUpperCase()));
        assertFalse(HashUtil.matches(null, "abc"));
    }
}
