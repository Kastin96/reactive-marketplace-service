package com.example.marketplace;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
    packages = "com.example.marketplace",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureRulesTests {

  @ArchTest
  static final ArchRule domain_should_not_depend_on_framework_or_adapter_layers =
      noClasses()
          .that().resideInAPackage("..domain..")
          .should().dependOnClassesThat().resideInAnyPackage(
              "org.springframework..",
              "jakarta..",
              "..api..",
              "..infrastructure.."
          );

  @ArchTest
  static final ArchRule api_should_not_depend_on_infrastructure =
      noClasses()
          .that().resideInAPackage("..api..")
          .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

  @ArchTest
  static final ArchRule application_should_not_depend_on_infrastructure =
      noClasses()
          .that().resideInAPackage("..application..")
          .should().dependOnClassesThat().resideInAPackage("..infrastructure..");
}
