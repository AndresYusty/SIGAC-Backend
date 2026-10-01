package com.universidad.sigac.service;

import com.universidad.sigac.config.AppProperties;
import com.universidad.sigac.exception.ApiException;
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
 * Almacenamiento local persistente (volumen /var/sigac/storage en Docker).
 * Todas las rutas que se guardan en base de datos son RELATIVAS a la raíz; se valida contra path traversal.
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
            throw new IllegalStateException("No fue posible crear el directorio de almacenamiento: " + base, e);
        }
    }

    public Path getBase() {
        return base;
    }

    /** Normaliza un segmento de ruta (sin tildes, sin separadores ni caracteres especiales). */
    public static String slug(String s) {
        if (s == null || s.isBlank()) {
            return "sin_nombre";
        }
        String n = Normalizer.normalize(s.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        n = n.replaceAll("[^A-Za-z0-9._-]+", "_").replaceAll("^[._]+", "");
        if (n.length() > 80) {
            n = n.substring(0, 80);
        }
        return n.isBlank() ? "sin_nombre" : n;
    }

    public Path resolve(String relativePath) {
        Path p = base.resolve(relativePath).normalize();
        if (!p.startsWith(base)) {
            throw ApiException.forbidden("Ruta de almacenamiento no permitida");
        }
        return p;
    }

    public String guardar(String relativePath, byte[] data) {
        Path p = resolve(relativePath);
        try {
            Files.createDirectories(p.getParent());
            Files.write(p, data);
        } catch (IOException e) {
            log.error("Error escribiendo {}", p, e);
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible guardar el archivo en el almacenamiento");
        }
        return relativePath;
    }

    public String guardar(String relativePath, InputStream in) {
        Path p = resolve(relativePath);
        try {
            Files.createDirectories(p.getParent());
            Files.copy(in, p, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Error escribiendo {}", p, e);
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible guardar el archivo en el almacenamiento");
        }
        return relativePath;
    }

    public byte[] leer(String relativePath) {
        Path p = resolve(relativePath);
        try {
            return Files.readAllBytes(p);
        } catch (IOException e) {
            throw ApiException.conflict("El archivo no se encuentra en el almacenamiento: " + relativePath);
        }
    }

    public boolean existe(String relativePath) {
        return Files.exists(resolve(relativePath));
    }

    public void eliminarSilencioso(String relativePath) {
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException | RuntimeException e) {
            log.warn("No se pudo eliminar {}", relativePath);
        }
    }
}
