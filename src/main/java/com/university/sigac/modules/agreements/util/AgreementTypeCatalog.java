package com.university.sigac.modules.agreements.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.university.sigac.core.exception.ApiException;
import com.university.sigac.modules.agreements.dto.AgreementDtos.AgreementTypeDto;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Agreement types taken from the Faculty Council models (adición de créditos, reingreso, homologación...).
 * They live in {@code sigac/agreements/types.json}: adding a type means adding an entry to that file.
 */
@Component
public class AgreementTypeCatalog {

    private static final String FILE = "/sigac/agreements/types.json";

    private final List<AgreementTypeDto> types;

    public AgreementTypeCatalog(ObjectMapper mapper) {
        try (InputStream in = AgreementTypeCatalog.class.getResourceAsStream(FILE)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + FILE);
            }
            types = List.copyOf(mapper.readValue(in, new TypeReference<List<AgreementTypeDto>>() { }));
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + FILE, e);
        }
    }

    public List<AgreementTypeDto> all() {
        return types;
    }

    public Optional<AgreementTypeDto> find(String code) {
        return types.stream().filter(t -> t.code().equals(code)).findFirst();
    }

    public AgreementTypeDto get(String code) {
        return find(code).orElseThrow(() -> ApiException.badRequest("Unknown agreement type: " + code));
    }
}
