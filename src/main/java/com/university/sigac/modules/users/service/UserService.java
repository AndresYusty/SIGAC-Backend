package com.university.sigac.modules.users.service;

import com.university.sigac.core.audit.AuditService;
import com.university.sigac.core.exception.ApiException;
import com.university.sigac.core.security.AuthenticatedUser;
import com.university.sigac.core.security.SecurityUtils;
import com.university.sigac.modules.users.dto.UserDtos.ChangePasswordRequest;
import com.university.sigac.modules.users.dto.UserDtos.UserCreateRequest;
import com.university.sigac.modules.users.dto.UserDtos.UserDto;
import com.university.sigac.modules.users.dto.UserDtos.UserUpdateRequest;
import com.university.sigac.modules.users.entity.Faculty;
import com.university.sigac.modules.users.entity.Program;
import com.university.sigac.modules.users.entity.User;
import com.university.sigac.modules.users.enums.RoleName;
import com.university.sigac.modules.users.repository.FacultyRepository;
import com.university.sigac.modules.users.repository.ProgramRepository;
import com.university.sigac.modules.users.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Module 1 – user and role administration (RF-04). */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository users;
    private final FacultyRepository faculties;
    private final ProgramRepository programs;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public Page<UserDto> list(String q, Pageable pageable) {
        Page<User> page = (q == null || q.isBlank()) ? users.findAll(pageable)
                : users.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(q.trim(), q.trim(), pageable);
        return page.map(UserService::toDto);
    }

    @Transactional(readOnly = true)
    public UserDto get(Long id) {
        return toDto(find(id));
    }

    @Transactional(readOnly = true)
    public UserDto currentProfile() {
        return toDto(find(SecurityUtils.current().id()));
    }

    @Transactional
    public UserDto create(UserCreateRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("A user with that email is already registered");
        }
        validatePassword(req.password());
        User u = new User();
        u.setName(req.name().trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.password()));
        assignRolesAndScope(u, req.roles(), req.facultyId(), req.programId());
        users.save(u);
        audit.record("EVENT_USER_CREATED", "USER", u.getId(), "Roles: " + u.getRoles());
        return toDto(u);
    }

    @Transactional
    public UserDto update(Long id, UserUpdateRequest req) {
        User u = find(id);
        u.setName(req.name().trim());
        assignRolesAndScope(u, req.roles(), req.facultyId(), req.programId());
        audit.record("EVENT_USER_UPDATED", "USER", u.getId(), "Roles: " + u.getRoles());
        return toDto(u);
    }

    /** Logical deactivation: a user is never physically deleted. */
    @Transactional
    public UserDto changeStatus(Long id, boolean active) {
        User u = find(id);
        if (!active && u.getId().equals(SecurityUtils.current().id())) {
            throw ApiException.badRequest("You cannot deactivate your own account");
        }
        u.setActive(active);
        audit.record(active ? "EVENT_USER_ACTIVATED" : "EVENT_USER_DEACTIVATED", "USER", u.getId(), null);
        return toDto(u);
    }

    @Transactional
    public void changeOwnPassword(ChangePasswordRequest req) {
        User u = find(SecurityUtils.current().id());
        if (!encoder.matches(req.currentPassword(), u.getPasswordHash())) {
            throw ApiException.badRequest("The current password is incorrect");
        }
        validatePassword(req.newPassword());
        u.setPasswordHash(encoder.encode(req.newPassword()));
        audit.record("EVENT_PASSWORD_CHANGED", "USER", u.getId(), null);
    }

    /** At least 8 characters, with at least one letter and one digit. */
    public static void validatePassword(String password) {
        if (password == null || password.length() < 8 || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw ApiException.badRequest("The password must be at least 8 characters long and include letters and numbers");
        }
    }

    // ---------------------------------------------------------------- mappings (also used by AuthService)

    public static UserDto toDto(User u) {
        Faculty f = u.getFaculty();
        Program p = u.getProgram();
        return new UserDto(u.getId(), u.getName(), u.getEmail(), u.isActive(), roleNames(u),
                f == null ? null : f.getId(), f == null ? null : f.getName(),
                p == null ? null : p.getId(), p == null ? null : p.getName(), u.getLastLogin());
    }

    public static AuthenticatedUser toAuthenticated(User u) {
        return new AuthenticatedUser(u.getId(), u.getEmail(), u.getName(), roleNames(u),
                u.getFaculty() == null ? null : u.getFaculty().getId(),
                u.getProgram() == null ? null : u.getProgram().getId());
    }

    private static List<String> roleNames(User u) {
        return u.getRoles().stream().map(Enum::name).sorted().toList();
    }

    // ---------------------------------------------------------------- private

    private User find(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
    }

    /** RN-01: operational roles require a Faculty; the Program, if given, must belong to that Faculty. */
    private void assignRolesAndScope(User u, Set<RoleName> roles, Long facultyId, Long programId) {
        if (roles.contains(RoleName.ROLE_DEV) && !SecurityUtils.current().hasRole("ROLE_DEV")) {
            throw ApiException.forbidden("Only the technical profile can assign the ROLE_DEV role");
        }
        Faculty faculty = facultyId == null ? null
                : faculties.findById(facultyId).orElseThrow(() -> ApiException.notFound("Faculty not found"));
        boolean onlyGlobal = roles.stream().allMatch(RoleName::isGlobal);
        if (!onlyGlobal && faculty == null) {
            throw ApiException.badRequest("RN-01: operational roles require a Faculty");
        }
        Program program = null;
        if (programId != null) {
            program = programs.findById(programId).orElseThrow(() -> ApiException.notFound("Program not found"));
            if (faculty == null || !program.getFaculty().getId().equals(faculty.getId())) {
                throw ApiException.badRequest("The Program does not belong to the given Faculty");
            }
        }
        u.setRoles(new HashSet<>(roles));
        u.setFaculty(faculty);
        u.setProgram(program);
    }
}
