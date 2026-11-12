package com.university.sigac.core.storage;

import com.university.sigac.config.AppProperties;
import com.university.sigac.core.exception.ApiException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Persistent storage: a local directory (sigac.storage.type=local) or an S3 bucket (type=s3).
 * Every path stored in the database is RELATIVE to the root; it is validated against path traversal.
 */
@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final FileStore store;

    public StorageService(AppProperties props) {
        AppProperties.Storage cfg = props.getStorage();
        if ("s3".equalsIgnoreCase(cfg.getType())) {
            this.store = new S3FileStore(cfg.getS3().getBucket(), cfg.getS3().getRegion());
            log.info("File storage: S3 bucket {}", cfg.getS3().getBucket());
        } else {
            this.store = new LocalFileStore(Paths.get(cfg.getBasePath()).toAbsolutePath().normalize());
            log.info("File storage: local directory {}", cfg.getBasePath());
        }
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

    /** Last segment of a stored path. */
    public static String fileName(String relativePath) {
        String key = relativePath.replace('\\', '/');
        return key.substring(key.lastIndexOf('/') + 1);
    }

    public String save(String relativePath, byte[] data) {
        String key = key(relativePath);
        try {
            store.write(key, data);
        } catch (IOException e) {
            log.error("Error writing {}", key, e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the file to storage");
        }
        return relativePath;
    }

    public String save(String relativePath, InputStream in) {
        String key = key(relativePath);
        try {
            store.write(key, in);
        } catch (IOException e) {
            log.error("Error writing {}", key, e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the file to storage");
        }
        return relativePath;
    }

    public byte[] read(String relativePath) {
        String key = key(relativePath);
        try {
            return store.read(key);
        } catch (FileNotFoundException e) {
            throw ApiException.conflict("The file was not found in storage: " + relativePath);
        } catch (IOException e) {
            log.error("Error reading {}", key, e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read the file from storage");
        }
    }

    public boolean exists(String relativePath) {
        return store.exists(key(relativePath));
    }

    /** Size in bytes, or -1 when the file does not exist or cannot be inspected. */
    public long size(String relativePath) {
        String key = key(relativePath);
        try {
            return store.size(key);
        } catch (IOException e) {
            log.warn("Could not read the size of {}", key);
            return -1;
        }
    }

    public void deleteQuietly(String relativePath) {
        try {
            store.delete(key(relativePath));
        } catch (IOException | RuntimeException e) {
            log.warn("Could not delete {}", relativePath);
        }
    }

    /** Relative, normalized key with "/" separators; rejects absolute paths and "..". */
    static String key(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw ApiException.forbidden("Storage path not allowed");
        }
        try {
            Path p = Paths.get(relativePath.replace('\\', '/')).normalize();
            String key = p.toString().replace('\\', '/');
            if (p.isAbsolute() || p.getRoot() != null || key.isEmpty() || key.equals("..") || key.startsWith("../")) {
                throw ApiException.forbidden("Storage path not allowed");
            }
            return key;
        } catch (InvalidPathException e) {
            throw ApiException.forbidden("Storage path not allowed");
        }
    }
}
