package com.university.sigac.modules.users.service;

import com.university.sigac.core.exception.ApiException;
import com.university.sigac.modules.users.dto.HierarchyDtos.CampusRequest;
import com.university.sigac.modules.users.dto.HierarchyDtos.FacultyRequest;
import com.university.sigac.modules.users.dto.HierarchyDtos.NodeDto;
import com.university.sigac.modules.users.dto.HierarchyDtos.ProgramRequest;
import com.university.sigac.modules.users.dto.HierarchyDtos.UniversityRequest;
import com.university.sigac.modules.users.entity.Campus;
import com.university.sigac.modules.users.entity.Faculty;
import com.university.sigac.modules.users.entity.Program;
import com.university.sigac.modules.users.entity.University;
import com.university.sigac.modules.agreements.repository.AgreementRepository;
import com.university.sigac.modules.minutes.repository.MinutesRepository;
import com.university.sigac.modules.requests.repository.CouncilRequestRepository;
import com.university.sigac.modules.requests.repository.SessionRepository;
import com.university.sigac.modules.templates.repository.MinutesTemplateRepository;
import com.university.sigac.modules.users.repository.CampusRepository;
import com.university.sigac.modules.users.repository.FacultyRepository;
import com.university.sigac.modules.users.repository.ProgramRepository;
import com.university.sigac.modules.users.repository.UniversityRepository;
import com.university.sigac.modules.users.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF-03 / RF-07: institutional hierarchy University > Campus > Faculty > Program. */
@Service
@RequiredArgsConstructor
@Transactional
public class HierarchyService {

    private final UniversityRepository universities;
    private final CampusRepository campuses;
    private final FacultyRepository faculties;
    private final ProgramRepository programs;
    private final UserRepository users;
    private final SessionRepository sessions;
    private final CouncilRequestRepository requests;
    private final MinutesRepository minutes;
    private final AgreementRepository agreements;
    private final MinutesTemplateRepository templates;

    // ---------------- Universities
    @Transactional(readOnly = true)
    public List<NodeDto> listUniversities() {
        return universities.findAll().stream().map(u -> new NodeDto(u.getId(), u.getName(), null, null)).toList();
    }

    public NodeDto createUniversity(UniversityRequest r) {
        if (universities.existsByNameIgnoreCase(r.name().trim())) {
            throw ApiException.conflict("A university with that name already exists");
        }
        University u = new University();
        u.setName(r.name().trim());
        universities.save(u);
        return new NodeDto(u.getId(), u.getName(), null, null);
    }

    public NodeDto updateUniversity(Long id, UniversityRequest r) {
        University u = universities.findById(id).orElseThrow(() -> ApiException.notFound("University not found"));
        String name = r.name().trim();
        if (universities.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw ApiException.conflict("A university with that name already exists");
        }
        u.setName(name);
        return new NodeDto(u.getId(), u.getName(), null, null);
    }

    public void deleteUniversity(Long id) {
        if (!universities.existsById(id)) {
            throw ApiException.notFound("University not found");
        }
        if (campuses.existsByUniversityId(id)) {
            throw ApiException.conflict("No se puede eliminar: esta universidad tiene sedes. Elimine primero lo que depende de ella.");
        }
        universities.deleteById(id);
    }

    // ---------------- Campuses
    @Transactional(readOnly = true)
    public List<NodeDto> listCampuses(Long universityId) {
        return campuses.findByUniversityIdOrderByName(universityId).stream().map(this::toDto).toList();
    }

    public NodeDto createCampus(CampusRequest r) {
        Campus s = new Campus();
        s.setUniversity(universities.findById(r.universityId()).orElseThrow(() -> ApiException.notFound("University not found")));
        s.setName(r.name().trim());
        s.setCity(r.city());
        return toDto(campuses.save(s));
    }

    public NodeDto updateCampus(Long id, CampusRequest r) {
        Campus s = campuses.findById(id).orElseThrow(() -> ApiException.notFound("Campus not found"));
        s.setName(r.name().trim());
        s.setCity(r.city());
        return toDto(s);
    }

    public void deleteCampus(Long id) {
        if (!campuses.existsById(id)) {
            throw ApiException.notFound("Campus not found");
        }
        if (faculties.existsByCampusId(id)) {
            throw ApiException.conflict("No se puede eliminar: esta sede tiene facultades. Elimine primero lo que depende de ella.");
        }
        campuses.deleteById(id);
    }

    // ---------------- Faculties
    @Transactional(readOnly = true)
    public List<NodeDto> listFaculties(Long campusId) {
        return faculties.findByCampusIdOrderByName(campusId).stream().map(this::toDto).toList();
    }

    public NodeDto createFaculty(FacultyRequest r) {
        Faculty f = new Faculty();
        f.setCampus(campuses.findById(r.campusId()).orElseThrow(() -> ApiException.notFound("Campus not found")));
        f.setName(r.name().trim());
        return toDto(faculties.save(f));
    }

    public NodeDto updateFaculty(Long id, FacultyRequest r) {
        Faculty f = faculties.findById(id).orElseThrow(() -> ApiException.notFound("Faculty not found"));
        f.setName(r.name().trim());
        return toDto(f);
    }

    public void deleteFaculty(Long id) {
        if (!faculties.existsById(id)) {
            throw ApiException.notFound("Faculty not found");
        }
        if (programs.existsByFacultyId(id) || users.existsByFacultyId(id) || sessions.existsByFacultyId(id)
                || requests.existsByFacultyId(id) || minutes.existsByFacultyId(id) || agreements.existsByFacultyId(id)
                || templates.existsByFacultyId(id)) {
            throw ApiException.conflict("No se puede eliminar: esta facultad tiene programas, usuarios o trámites del consejo.");
        }
        faculties.deleteById(id);
    }

    // ---------------- Programs
    @Transactional(readOnly = true)
    public List<NodeDto> listPrograms(Long facultyId) {
        return programs.findByFacultyIdOrderByName(facultyId).stream().map(this::toDto).toList();
    }

    public NodeDto createProgram(ProgramRequest r) {
        Program p = new Program();
        p.setFaculty(faculties.findById(r.facultyId()).orElseThrow(() -> ApiException.notFound("Faculty not found")));
        p.setName(r.name().trim());
        return toDto(programs.save(p));
    }

    public NodeDto updateProgram(Long id, ProgramRequest r) {
        Program p = programs.findById(id).orElseThrow(() -> ApiException.notFound("Program not found"));
        p.setName(r.name().trim());
        return toDto(p);
    }

    public void deleteProgram(Long id) {
        if (!programs.existsById(id)) {
            throw ApiException.notFound("Program not found");
        }
        if (users.existsByProgramId(id) || requests.existsByProgramId(id)) {
            throw ApiException.conflict("No se puede eliminar: hay personas o solicitudes en este programa.");
        }
        programs.deleteById(id);
    }

    // ---------------- Lookups used by other modules

    /** Proxy of an existing Faculty, to link it to another entity without loading it. */
    public Faculty facultyReference(Long id) {
        return faculties.getReferenceById(id);
    }

    @Transactional(readOnly = true)
    public Faculty findFaculty(Long id) {
        return faculties.findById(id).orElseThrow(() -> ApiException.notFound("Faculty not found"));
    }

    @Transactional(readOnly = true)
    public Program findProgram(Long id) {
        return programs.findById(id).orElseThrow(() -> ApiException.notFound("Program not found"));
    }

    private NodeDto toDto(Campus s) {
        return new NodeDto(s.getId(), s.getName(), s.getUniversity().getId(), s.getCity());
    }

    private NodeDto toDto(Faculty f) {
        return new NodeDto(f.getId(), f.getName(), f.getCampus().getId(), null);
    }

    private NodeDto toDto(Program p) {
        return new NodeDto(p.getId(), p.getName(), p.getFaculty().getId(), null);
    }
}
