package com.university.sigac.controller;

import com.university.sigac.dto.ConfigDtos.CampusRequest;
import com.university.sigac.dto.ConfigDtos.FacultyRequest;
import com.university.sigac.dto.ConfigDtos.NodeDto;
import com.university.sigac.dto.ConfigDtos.ProgramRequest;
import com.university.sigac.dto.ConfigDtos.UniversityRequest;
import com.university.sigac.service.HierarchyService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** University > Campus > Faculty > Program hierarchy. Read: any authenticated user. Write: ADMIN. */
@RestController
@RequestMapping("/api/v1/hierarchy")
@RequiredArgsConstructor
public class HierarchyController {

    private final HierarchyService service;

    @GetMapping("/universities")
    public List<NodeDto> universities() {
        return service.listUniversities();
    }

    @PostMapping("/universities")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public NodeDto createUniversity(@Valid @RequestBody UniversityRequest r) {
        return service.createUniversity(r);
    }

    @PutMapping("/universities/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public NodeDto updateUniversity(@PathVariable Long id, @Valid @RequestBody UniversityRequest r) {
        return service.updateUniversity(id, r);
    }

    @GetMapping("/campuses")
    public List<NodeDto> campuses(@RequestParam Long universityId) {
        return service.listCampuses(universityId);
    }

    @PostMapping("/campuses")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public NodeDto createCampus(@Valid @RequestBody CampusRequest r) {
        return service.createCampus(r);
    }

    @PutMapping("/campuses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public NodeDto updateCampus(@PathVariable Long id, @Valid @RequestBody CampusRequest r) {
        return service.updateCampus(id, r);
    }

    @GetMapping("/faculties")
    public List<NodeDto> faculties(@RequestParam Long campusId) {
        return service.listFaculties(campusId);
    }

    @PostMapping("/faculties")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public NodeDto createFaculty(@Valid @RequestBody FacultyRequest r) {
        return service.createFaculty(r);
    }

    @PutMapping("/faculties/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public NodeDto updateFaculty(@PathVariable Long id, @Valid @RequestBody FacultyRequest r) {
        return service.updateFaculty(id, r);
    }

    @GetMapping("/programs")
    public List<NodeDto> programs(@RequestParam Long facultyId) {
        return service.listPrograms(facultyId);
    }

    @PostMapping("/programs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public NodeDto createProgram(@Valid @RequestBody ProgramRequest r) {
        return service.createProgram(r);
    }

    @PutMapping("/programs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public NodeDto updateProgram(@PathVariable Long id, @Valid @RequestBody ProgramRequest r) {
        return service.updateProgram(id, r);
    }
}
