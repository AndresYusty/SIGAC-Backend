package com.university.sigac.modules.users.repository;

import com.university.sigac.modules.users.entity.User;
import com.university.sigac.modules.users.enums.RoleName;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByFacultyId(Long facultyId);

    boolean existsByProgramId(Long programId);

    boolean existsByIdAndActiveTrue(Long id);

    long countByActive(boolean active);

    Page<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email, Pageable pageable);

    /** Active users of a Faculty holding any of the given roles (quorum and email recipients). */
    @Query("select distinct u from User u join u.roles r where u.faculty.id = :facultyId and u.active = true "
            + "and r in :roles order by u.name")
    List<User> findActiveByFacultyAndRoles(@Param("facultyId") Long facultyId, @Param("roles") Collection<RoleName> roles);

    @Query("select distinct u from User u join u.roles r where u.active = true and r in :roles")
    List<User> findActiveByRoles(@Param("roles") Collection<RoleName> roles);
}
