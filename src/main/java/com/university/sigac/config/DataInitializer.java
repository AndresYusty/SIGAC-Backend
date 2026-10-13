package com.university.sigac.config;

import com.university.sigac.core.parameter.ParameterService;
import com.university.sigac.modules.templates.service.TemplateService;
import com.university.sigac.modules.users.entity.Campus;
import com.university.sigac.modules.users.entity.CouncilType;
import com.university.sigac.modules.users.entity.Faculty;
import com.university.sigac.modules.users.entity.Program;
import com.university.sigac.modules.users.entity.University;
import com.university.sigac.modules.users.entity.User;
import com.university.sigac.modules.users.enums.RoleName;
import com.university.sigac.modules.users.repository.CampusRepository;
import com.university.sigac.modules.users.repository.CouncilTypeRepository;
import com.university.sigac.modules.users.repository.FacultyRepository;
import com.university.sigac.modules.users.repository.ProgramRepository;
import com.university.sigac.modules.users.repository.UniversityRepository;
import com.university.sigac.modules.users.repository.UserRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Idempotent initial load: council types, parameters, technical users, default template and demo data. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final CouncilTypeRepository councilTypes;
    private final ParameterService parameters;
    private final UserRepository users;
    private final UniversityRepository universities;
    private final CampusRepository campuses;
    private final FacultyRepository faculties;
    private final ProgramRepository programs;
    private final TemplateService templates;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createCouncilType("CF", "Faculty Council");
        createCouncilType("CA", "Academic Council");
        createCouncilType("CS", "Superior Council");

        parameters.ensure(ParameterService.ATTACHMENTS_MAX_MB, "10", "Maximum size per attached file (MB)");
        parameters.ensure(ParameterService.QUORUM_PERCENTAGE, "50",
                "Base percentage for the deliberative quorum (minimum = percentage of members + 1)");

        createUser("SIGAC Administrator", props.getSeed().getAdminEmail(), props.getSeed().getAdminPassword(), RoleName.ROLE_ADMIN, null, null);
        createUser("SIGAC Developer", props.getSeed().getDevEmail(), props.getSeed().getDevPassword(), RoleName.ROLE_DEV, null, null);
        councilTypes.findAll().forEach(templates::createDefaultIfMissing);

        if (props.getSeed().isDemoData() && universities.count() == 0) {
            loadDemo();
        }
    }

    private void createCouncilType(String code, String name) {
        if (councilTypes.findByCode(code).isEmpty()) {
            councilTypes.save(new CouncilType(code, name));
        }
    }

    private void createUser(String name, String email, String password, RoleName role, Faculty faculty, Program program) {
        if (users.existsByEmailIgnoreCase(email)) {
            return;
        }
        User u = new User();
        u.setName(name);
        u.setEmail(email.toLowerCase());
        u.setPasswordHash(encoder.encode(password));
        u.setRoles(new java.util.HashSet<>(Set.of(role)));
        u.setFaculty(faculty);
        u.setProgram(program);
        users.save(u);
        log.warn("User created: {} ({}). Change the initial password.", email, role);
    }

    private void loadDemo() {
        University uni = new University();
        uni.setName("Demo University");
        universities.save(uni);
        Campus campus = new Campus();
        campus.setName("Main Campus");
        campus.setCity("Bogota");
        campus.setUniversity(uni);
        campuses.save(campus);
        Faculty fac = new Faculty();
        fac.setName("Faculty of Engineering");
        fac.setCampus(campus);
        faculties.save(fac);
        Program prog = new Program();
        prog.setName("Systems Engineering");
        prog.setFaculty(fac);
        programs.save(prog);

        String password = "Demo12345*";
        createUser("Demo Managing Secretary", "sec1@demo.edu", password, RoleName.ROLE_SECRETARY_1, fac, null);
        createUser("Demo Assistant Secretary", "sec2@demo.edu", password, RoleName.ROLE_SECRETARY_2, fac, prog);
        createUser("Demo President (Dean)", "presidente@demo.edu", password, RoleName.ROLE_PRESIDENT, fac, null);
        createUser("Council Member One", "consejero1@demo.edu", password, RoleName.ROLE_COUNCIL_MEMBER, fac, null);
        createUser("Council Member Two", "consejero2@demo.edu", password, RoleName.ROLE_COUNCIL_MEMBER, fac, null);
        createUser("Council Member Three", "consejero3@demo.edu", password, RoleName.ROLE_COUNCIL_MEMBER, fac, null);
        log.warn("DEMO data loaded (demo user password: {}). Do not use in production.", password);
    }
}
