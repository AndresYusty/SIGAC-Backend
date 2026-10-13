package com.university.sigac.controller;

import com.university.sigac.dto.ConfigDtos.CouncilTypeDto;
import com.university.sigac.dto.ConfigDtos.CouncilTypeRequest;
import com.university.sigac.dto.ConfigDtos.ParameterDto;
import com.university.sigac.dto.ConfigDtos.ParameterRequest;
import com.university.sigac.service.CouncilTypeService;
import com.university.sigac.service.ParameterService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Council type catalog (RF-08) and global system parameters (RF-09). */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ConfigurationController {

    private final CouncilTypeService councilTypes;
    private final ParameterService parameters;

    @GetMapping("/council-types")
    public List<CouncilTypeDto> listCouncilTypes() {
        return councilTypes.list();
    }

    @PostMapping("/council-types")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CouncilTypeDto createCouncilType(@Valid @RequestBody CouncilTypeRequest r) {
        return councilTypes.create(r);
    }

    @PutMapping("/council-types/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CouncilTypeDto updateCouncilType(@PathVariable Long id, @Valid @RequestBody CouncilTypeRequest r) {
        return councilTypes.update(id, r);
    }

    @GetMapping("/parameters")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ParameterDto> listParameters() {
        return parameters.list();
    }

    @PutMapping("/parameters/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ParameterDto updateParameter(@PathVariable String key, @Valid @RequestBody ParameterRequest r) {
        return parameters.update(key, r.value());
    }
}
