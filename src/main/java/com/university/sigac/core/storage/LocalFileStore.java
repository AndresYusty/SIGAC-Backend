package com.university.sigac.core.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Directory on the local disk (volume /var/sigac/storage in Docker). */
final class LocalFileStore implements FileStore {

    private final Path base;

    LocalFileStore(Path base) {
        this.base = base;
        try {
            Files.createDirectories(base);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create the storage directory: " + base, e);
        }
    }

    @Override
    public void write(String key, byte[] data) throws IOException {
        Path p = path(key);
        Files.createDirectories(p.getParent());
        Files.write(p, data);
    }

    @Override
    public void write(String key, InputStream in) throws IOException {
        Path p = path(key);
        Files.createDirectories(p.getParent());
        Files.copy(in, p, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public byte[] read(String key) throws IOException {
        try {
            return Files.readAllBytes(path(key));
        } catch (NoSuchFileException e) {
            throw new FileNotFoundException(key);
        }
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(path(key));
    }

    @Override
    public long size(String key) throws IOException {
        Path p = path(key);
        return Files.exists(p) ? Files.size(p) : -1;
    }

    @Override
    public void delete(String key) throws IOException {
        Files.deleteIfExists(path(key));
    }

    private Path path(String key) {
        Path p = base.resolve(key).normalize();
        if (!p.startsWith(base)) {
            throw new IllegalArgumentException("Storage path outside the base directory: " + key);
        }
        return p;
    }
}
