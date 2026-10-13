package com.university.sigac.service;

import com.university.sigac.dto.ConfigDtos.ParameterDto;
import com.university.sigac.entity.Parameter;
import com.university.sigac.exception.ApiException;
import com.university.sigac.repository.ParameterRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Global parameters (RN-04): read from the database, so they can be changed without redeploying. */
@Service
@RequiredArgsConstructor
public class ParameterService {

    public static final String ATTACHMENTS_MAX_MB = "attachments.max_mb";
    public static final String QUORUM_PERCENTAGE = "quorum.percentage";

    private final ParameterRepository repository;

    @Transactional(readOnly = true)
    public int getInt(String key, int defaultValue) {
        return repository.findById(key).map(p -> {
            try {
                return Integer.parseInt(p.getParamValue().trim());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }).orElse(defaultValue);
    }

    @Transactional(readOnly = true)
    public List<ParameterDto> list() {
        return repository.findAll().stream()
                .map(p -> new ParameterDto(p.getParamKey(), p.getParamValue(), p.getDescription())).toList();
    }

    @Transactional
    public ParameterDto update(String key, String value) {
        Parameter p = repository.findById(key).orElseThrow(() -> ApiException.notFound("Parameter not found: " + key));
        int n;
        try {
            n = Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("The value must be an integer");
        }
        if (n <= 0 || (QUORUM_PERCENTAGE.equals(key) && n > 100)) {
            throw ApiException.badRequest("Value out of range for " + key);
        }
        p.setParamValue(String.valueOf(n));
        return new ParameterDto(p.getParamKey(), p.getParamValue(), p.getDescription());
    }

    /** Creates the parameter with its default value if it does not exist yet (initial load). */
    @Transactional
    public void ensure(String key, String value, String description) {
        if (!repository.existsById(key)) {
            repository.save(new Parameter(key, value, description));
        }
    }
}
