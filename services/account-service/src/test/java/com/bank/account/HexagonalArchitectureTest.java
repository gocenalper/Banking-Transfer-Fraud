package com.bank.account;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

/**
 * Hexagonal layering rules inside this service (Clean Architecture ch. 22, "The Dependency Rule"):
 * - domain may not depend on any framework,
 * - application may depend only on domain,
 * - adapters may depend on application and domain, never the other way around.
 */
@AnalyzeClasses(packages = "com.bank.account")
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule domainIsFrameworkFree = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "com.fasterxml.jackson..")
            .because("the domain must stay unaware of frameworks; persistence and serialization belong to adapters")
            .allowEmptyShould(true); // tolerate an empty match until the layers are populated

    @ArchTest
    static final ArchRule hexagonalLayers = onionArchitecture()
            .withOptionalLayers(true)
            .domainModels("..domain..")
            .applicationServices("..application..")
            .adapter("web", "..adapter.in.web..")
            .adapter("messaging-in", "..adapter.in.messaging..")
            .adapter("persistence", "..adapter.out.persistence..")
            .adapter("messaging-out", "..adapter.out.messaging..");
}
