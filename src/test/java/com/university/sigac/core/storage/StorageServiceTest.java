package com.university.sigac.core.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.university.sigac.config.AppProperties;
import com.university.sigac.core.exception.ApiException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StorageServiceTest {

    @TempDir
    Path dir;

    private StorageService local() {
        AppProperties props = new AppProperties();
        props.getStorage().setBasePath(dir.toString());
        return new StorageService(props);
    }

    @Test
    void localRoundTrip() {
        StorageService storage = local();
        byte[] data = "pdf".getBytes(StandardCharsets.UTF_8);
        storage.save("minutes/2026/MIN-1.pdf", data);

        assertTrue(storage.exists("minutes/2026/MIN-1.pdf"));
        assertArrayEquals(data, storage.read("minutes/2026/MIN-1.pdf"));
        assertEquals(3, storage.size("minutes/2026/MIN-1.pdf"));

        storage.deleteQuietly("minutes/2026/MIN-1.pdf");
        assertFalse(storage.exists("minutes/2026/MIN-1.pdf"));
        assertEquals(-1, storage.size("minutes/2026/MIN-1.pdf"));
        assertThrows(ApiException.class, () -> storage.read("minutes/2026/MIN-1.pdf"));
    }

    @Test
    void keysAreRelativeAndNormalized() {
        assertEquals("a/b/c.pdf", StorageService.key("a\\b\\c.pdf"));
        assertEquals("a/c.pdf", StorageService.key("a/./b/../c.pdf"));
        assertThrows(ApiException.class, () -> StorageService.key("../secret"));
        assertThrows(ApiException.class, () -> StorageService.key("a/../../secret"));
        assertThrows(ApiException.class, () -> StorageService.key("/etc/passwd"));
        assertThrows(ApiException.class, () -> StorageService.key(""));
    }

    @Test
    void fileNameIsTheLastSegment() {
        assertEquals("citacion.pdf", StorageService.fileName("citaciones/17/citacion.pdf"));
    }
}
