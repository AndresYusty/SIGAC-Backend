package com.university.sigac.core.parameter;

import com.university.sigac.core.parameter.ParameterDtos.ParameterDto;
import com.university.sigac.core.parameter.ParameterDtos.ParameterRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Global system parameters (RF-09). */
@RestController
@RequestMapping("/api/v1/parameters")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ParameterController {

    private final ParameterService parameters;

    @GetMapping
    public List<ParameterDto> list() {
        return parameters.list();
    }

    @PutMapping("/{key}")
    public ParameterDto update(@PathVariable String key, @Valid @RequestBody ParameterRequest r) {
        return parameters.update(key, r.value());
    }
}
