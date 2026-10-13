package com.university.sigac.modules.users.service;

import com.university.sigac.core.exception.ApiException;
import com.university.sigac.modules.users.dto.CouncilTypeDtos.CouncilTypeDto;
import com.university.sigac.modules.users.dto.CouncilTypeDtos.CouncilTypeRequest;
import com.university.sigac.modules.users.entity.CouncilType;
import com.university.sigac.modules.users.repository.CouncilTypeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF-08: catalog of council types (Faculty, Academic, Superior). */
@Service
@RequiredArgsConstructor
public class CouncilTypeService {

    private final CouncilTypeRepository repository;

    @Transactional(readOnly = true)
    public List<CouncilTypeDto> list() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public CouncilTypeDto create(CouncilTypeRequest r) {
        String code = r.code().trim().toUpperCase();
        if (repository.findByCode(code).isPresent()) {
            throw ApiException.conflict("A council type with that code already exists");
        }
        return toDto(repository.save(new CouncilType(code, r.name().trim())));
    }

    /** The code cannot be changed: it is part of the serial numbers already issued (RN-13). */
    @Transactional
    public CouncilTypeDto update(Long id, CouncilTypeRequest r) {
        CouncilType t = find(id);
        t.setName(r.name().trim());
        return toDto(t);
    }

    /** Used by other modules (templates, sessions) to resolve the council type of an operation. */
    @Transactional(readOnly = true)
    public CouncilType find(Long id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("Council type not found"));
    }

    private CouncilTypeDto toDto(CouncilType t) {
        return new CouncilTypeDto(t.getId(), t.getCode(), t.getName());
    }
}
