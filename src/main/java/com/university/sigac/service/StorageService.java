package com.university.sigac.service;

import com.university.sigac.config.AppProperties;
import com.university.sigac.exception.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Persistent local storage (volume /var/sigac/storage in Docker).
 * Every path stored in the database is RELATIVE to the root; it is validated against path traversal.
 */
@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final Path base;

    public StorageService(AppProperties props) {
        this.base = Paths.get(props.getStorage().getBasePath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(base);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create the storage directory: " + base, e);
        }
    }

    public Path getBase() {
        return base;
    }

    /** Normalizes a path segment (no diacritics, separators or special characters). */
    public static String slug(String s) {
        if (s == null || s.isBlank()) {
            return "unnamed";
        }
        String n = Normalizer.normalize(s.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        n = n.replaceAll("[^A-Za-z0-9._-]+", "_").replaceAll("^[._]+", "");
        if (n.length() > 80) {
            n = n.substring(0, 80);
        }
        return n.isBlank() ? "unnamed" : n;
    }

    public Path resolve(String relativePath) {
        Path p = base.resolve(relativePath).normalize();
        if (!p.startsWith(base)) {
            throw ApiException.forbidden("Storage path not allowed");
        }
        return p;
    }

    public String save(String relativePath, byte[] data) {
        Path p = resolve(relativePath);
        try {
            Files.createDirectories(p.getParent());
            Files.write(p, data);
        } catch (IOException e) {
            log.error("Error writing {}", p, e);
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not save the file to storage");
        }
        return relativePath;
    }

    public String save(String relativePath, InputStream in) {
        Path p = resolve(relativePath);
        try {
            Files.createDirectories(p.getParent());
            Files.copy(in, p, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Error writing {}", p, e);
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not save the file to storage");
        }
        return relativePath;
    }

    public byte[] read(String relativePath) {
        Path p = resolve(relativePath);
        try {
            return Files.readAllBytes(p);
        } catch (IOException e) {
            throw ApiException.conflict("The file was not found in storage: " + relativePath);
        }
    }

    public boolean exists(String relativePath) {
        return Files.exists(resolve(relativePath));
    }

    public void deleteQuietly(String relativePath) {
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException | RuntimeException e) {
            log.warn("Could not delete {}", relativePath);
        }
    }
}
