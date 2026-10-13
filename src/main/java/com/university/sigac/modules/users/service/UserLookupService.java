package com.university.sigac.modules.users.service;

import com.university.sigac.core.security.ActiveUserChecker;
import com.university.sigac.modules.users.entity.User;
import com.university.sigac.modules.users.enums.RoleName;
import com.university.sigac.modules.users.repository.UserRepository;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Read-only user queries offered to the other modules, so that UserRepository stays internal to this module. */
@Service
@RequiredArgsConstructor
public class UserLookupService implements ActiveUserChecker {

    private final UserRepository users;

    @Override
    public boolean isActive(Long userId) {
        return users.existsByIdAndActiveTrue(userId);
    }

    /** Proxy of an existing user, to link it to another entity without loading it. */
    public User reference(Long userId) {
        return users.getReferenceById(userId);
    }

    /** Active users of a Faculty holding any of the given roles (quorum and email recipients). */
    public List<User> findActiveByFacultyAndRoles(Long facultyId, Collection<RoleName> roles) {
        return users.findActiveByFacultyAndRoles(facultyId, roles);
    }

    public List<String> activeEmailsByFacultyAndRoles(Long facultyId, Collection<RoleName> roles) {
        return users.findActiveByFacultyAndRoles(facultyId, roles).stream().map(User::getEmail).distinct().toList();
    }

    public List<String> activeEmailsByRoles(Collection<RoleName> roles) {
        return users.findActiveByRoles(roles).stream().map(User::getEmail).distinct().toList();
    }

    public long countByActive(boolean active) {
        return users.countByActive(active);
    }
}
