package com.university.sigac;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.university.sigac.config.AppProperties;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

/** Guards the modular monolith boundaries: core → nothing, modules → only the modules they declare. */
class ModularArchitectureTest {

    private static final String BASE = "com.university.sigac";
    private static final String CORE = BASE + ".core..";
    private static final String CONFIG = BASE + ".config..";
    private static final String MODULES = BASE + ".modules.";

    /** Allowed dependencies between business modules (core is available to all of them). */
    private static final Map<String, Set<String>> ALLOWED = Map.of(
            "notifications", Set.of(),
            "users", Set.of("notifications"),
            "templates", Set.of("users"),
            "requests", Set.of("users", "notifications"),
            "session", Set.of("requests", "users"),
            "minutes", Set.of("session", "requests", "templates", "users", "notifications"),
            "repository", Set.of("minutes", "session", "requests", "users", "notifications"));

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE);

    @Test
    void everyModuleDeclaresItsDependencies() {
        Set<String> found = CLASSES.stream().map(JavaClass::getPackageName)
                .filter(p -> p.startsWith(MODULES))
                .map(p -> p.substring(MODULES.length()).split("\\.")[0])
                .collect(Collectors.toCollection(TreeSet::new));
        assertEquals(new TreeSet<>(ALLOWED.keySet()), found);
    }

    @Test
    void coreDoesNotDependOnModules() {
        noClasses().that().resideInAPackage(CORE)
                .should().dependOnClassesThat().resideInAPackage(MODULES + ".")
                .because("core is the shared kernel: modules depend on it, never the other way around")
                .check(CLASSES);
    }

    @Test
    void coreAndModulesOnlyUseApplicationPropertiesFromConfig() {
        noClasses().that().resideInAnyPackage(CORE, MODULES + ".")
                .should().dependOnClassesThat(resideInAPackage(CONFIG).and(not(belongToAnyOf(AppProperties.class))))
                .because("config wires Spring beans; business code only reads the typed properties")
                .check(CLASSES);
    }

    @Test
    void modulesOnlyDependOnTheirDeclaredModules() {
        ALLOWED.forEach((module, allowed) -> {
            String[] forbidden = ALLOWED.keySet().stream()
                    .filter(other -> !other.equals(module) && !allowed.contains(other))
                    .map(other -> MODULES + other + "..")
                    .toArray(String[]::new);
            if (forbidden.length > 0) {
                noClasses().that().resideInAPackage(MODULES + module + "..")
                        .should().dependOnClassesThat().resideInAnyPackage(forbidden)
                        .because("module '" + module + "' may only use " + (allowed.isEmpty() ? "core" : allowed))
                        .check(CLASSES);
            }
        });
    }

    @Test
    void modulesAreFreeOfCycles() {
        slices().matching(MODULES + "(*)..").should().beFreeOfCycles().check(CLASSES);
    }

    @Test
    void repositoriesArePrivateToTheirModule() {
        ALLOWED.keySet().forEach(module -> noClasses()
                .that().resideInAPackage(MODULES + ".")
                .and().resideOutsideOfPackage(MODULES + module + "..")
                .should().dependOnClassesThat().resideInAPackage(MODULES + module + ".repository..")
                .because("other modules must go through the public services of '" + module + "'")
                .check(CLASSES));
    }

    @Test
    void controllersAreOnlyEntryPoints() {
        noClasses().should().dependOnClassesThat().areAnnotatedWith(RestController.class)
                .because("controllers expose HTTP endpoints and are never called from other classes")
                .check(CLASSES);
    }
}
