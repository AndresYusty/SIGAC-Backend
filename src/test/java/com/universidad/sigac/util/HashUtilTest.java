package com.universidad.sigac.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.universidad.sigac.util.HashUtil;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HashUtilTest {

    @Test
    void sha256ConocidoDeAbc() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", HashUtil.sha256Hex("abc"));
    }

    @Test
    void hashDeArchivoCoincideConHashDeBytes(@TempDir Path dir) throws Exception {
        byte[] datos = "contenido del acta".getBytes(StandardCharsets.UTF_8);
        Path f = dir.resolve("acta.pdf");
        Files.write(f, datos);
        assertEquals(HashUtil.sha256Hex(datos), HashUtil.sha256Hex(f));
    }

    @Test
    void cualquierAlteracionCambiaElHash() {
        assertFalse(HashUtil.iguales(HashUtil.sha256Hex("acta v1"), HashUtil.sha256Hex("acta v1 ")));
        assertTrue(HashUtil.iguales(HashUtil.sha256Hex("x"), HashUtil.sha256Hex("x").toUpperCase()));
        assertFalse(HashUtil.iguales(null, "abc"));
    }
}
