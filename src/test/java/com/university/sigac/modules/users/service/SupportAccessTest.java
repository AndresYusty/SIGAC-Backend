package com.university.sigac.modules.users.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.university.sigac.modules.users.entity.Faculty;
import com.university.sigac.modules.users.entity.Program;
import com.university.sigac.modules.users.entity.User;
import com.university.sigac.modules.users.enums.RoleName;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SupportAccessTest {

    @Test
    void deanManagesSecretariesOfTheSameFaculty() {
        Faculty engineering = faculty(1L);
        Faculty education = faculty(2L);
        User dean = user(10L, RoleName.ROLE_PRESIDENT, engineering, null);
        User council = user(11L, RoleName.ROLE_COUNCIL_SECRETARY, engineering, null);
        User programSecretary = user(12L, RoleName.ROLE_FACULTY_SECRETARY, engineering, program(5L, engineering));
        User otherFaculty = user(13L, RoleName.ROLE_COUNCIL_SECRETARY, education, null);
        User counselor = user(14L, RoleName.ROLE_COUNCIL_MEMBER, engineering, null);

        assertTrue(SupportAccess.canManage(dean, council));
        assertTrue(SupportAccess.canManage(dean, programSecretary));
        assertFalse(SupportAccess.canManage(dean, otherFaculty));
        assertFalse(SupportAccess.canManage(dean, counselor));
        assertFalse(SupportAccess.canManage(dean, dean));
    }

    @Test
    void programHeadManagesOnlyTheSecretaryOfThatProgram() {
        Faculty engineering = faculty(1L);
        Program software = program(5L, engineering);
        Program civil = program(6L, engineering);
        User head = user(20L, RoleName.ROLE_PROGRAM_HEAD, engineering, software);
        User ownSecretary = user(21L, RoleName.ROLE_FACULTY_SECRETARY, engineering, software);
        User otherSecretary = user(22L, RoleName.ROLE_FACULTY_SECRETARY, engineering, civil);
        User council = user(23L, RoleName.ROLE_COUNCIL_SECRETARY, engineering, null);

        assertTrue(SupportAccess.canManage(head, ownSecretary));
        assertFalse(SupportAccess.canManage(head, otherSecretary));
        assertFalse(SupportAccess.canManage(head, council));
    }

    @Test
    void adminCanManageAnyAccount() {
        Faculty engineering = faculty(1L);
        User admin = user(1L, RoleName.ROLE_ADMIN, null, null);
        User counselor = user(14L, RoleName.ROLE_COUNCIL_MEMBER, engineering, null);
        assertTrue(SupportAccess.canManage(admin, counselor));
    }

    private static Faculty faculty(Long id) {
        Faculty faculty = new Faculty();
        faculty.setId(id);
        return faculty;
    }

    private static Program program(Long id, Faculty faculty) {
        Program program = new Program();
        program.setId(id);
        program.setFaculty(faculty);
        return program;
    }

    private static User user(Long id, RoleName role, Faculty faculty, Program program) {
        User user = new User();
        user.setId(id);
        user.setRoles(Set.of(role));
        user.setFaculty(faculty);
        user.setProgram(program);
        return user;
    }
}
