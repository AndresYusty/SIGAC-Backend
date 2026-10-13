package com.university.sigac.modules.users.controller;

import com.university.sigac.modules.users.dto.CouncilTypeDtos.CouncilTypeDto;
import com.university.sigac.modules.users.dto.CouncilTypeDtos.CouncilTypeRequest;
import com.university.sigac.modules.users.service.CouncilTypeService;
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

/** Council type catalog (RF-08). */
@RestController
@RequestMapping("/api/v1/council-types")
@RequiredArgsConstructor
public class CouncilTypeController {

    private final CouncilTypeService councilTypes;

    @GetMapping
    public List<CouncilTypeDto> list() {
        return councilTypes.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CouncilTypeDto create(@Valid @RequestBody CouncilTypeRequest r) {
        return councilTypes.create(r);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CouncilTypeDto update(@PathVariable Long id, @Valid @RequestBody CouncilTypeRequest r) {
        return councilTypes.update(id, r);
    }
}
