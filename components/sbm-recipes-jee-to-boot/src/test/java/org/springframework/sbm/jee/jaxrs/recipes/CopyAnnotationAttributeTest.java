package org.springframework.sbm.jee.jaxrs.recipes;

import org.junit.jupiter.api.Test;
import org.springframework.sbm.engine.context.ProjectContext;
import org.springframework.sbm.java.api.JavaSource;
import org.springframework.sbm.project.resource.TestProjectContext;

import static org.assertj.core.api.Assertions.assertThat;

class CopyAnnotationAttributeTest {
    private final static String SPRING_VERSION = "5.3.13";

    @Test
    void givenBothAnnotationsArePresent_thenTheAttributeIsCopied() {
        String sourceCode = """                
                import org.springframework.web.bind.annotation.RequestParam;
                import javax.ws.rs.DefaultValue;
                                
                class ControllerClass {
                    public String methodWithDefaultedParameters(@DefaultValue("default-value") @RequestParam(value = "q") String searchString) {
                        return "Hello";
                    }
                }
                """;

        String expected = """
                import org.springframework.web.bind.annotation.RequestParam;
                import javax.ws.rs.DefaultValue;
                                
                class ControllerClass {
                    public String methodWithDefaultedParameters(@DefaultValue("default-value") @RequestParam(defaultValue = "default-value", value = "q") String searchString) {
                        return "Hello";
                    }
                }
                """;

        testCopyAnnotationAttribute(sourceCode, expected);
    }

    @Test
    void givenTheTargetAnnotationIsPositionedBeforeTheSourceAnnotation_thenTheAttributeIsCopied() {
        String sourceCode = """                
                import org.springframework.web.bind.annotation.RequestParam;
                import javax.ws.rs.DefaultValue;
                                
                class ControllerClass {
                    public String methodWithDefaultedParameters(@RequestParam(value = "q") @DefaultValue("default-value") String searchString) {
                        return "Hello";
                    }
                }
                """;

        String expected = """
                import org.springframework.web.bind.annotation.RequestParam;
                import javax.ws.rs.DefaultValue;
                                
                class ControllerClass {
                    public String methodWithDefaultedParameters(@RequestParam(defaultValue = "default-value", value = "q") @DefaultValue("default-value") String searchString) {
                        return "Hello";
                    }
                }
                """;

        testCopyAnnotationAttribute(sourceCode, expected);
    }

    @Test
    void givenTheTargetAnnotationOnlyHasALiteralValueAndTheTargetAttributeIsNotValue_thenTheTargetAnnotationIsExpandedAndTheAttributeCopied() {
        String sourceCode = """                
                import org.springframework.web.bind.annotation.RequestParam;
                import javax.ws.rs.DefaultValue;
                                
                class ControllerClass {
                    public String methodWithDefaultedParameters(@DefaultValue("default-value") @RequestParam("q") String searchString) {
                        return "Hello";
                    }
                }
                """;

        String expected = """
                import org.springframework.web.bind.annotation.RequestParam;
                import javax.ws.rs.DefaultValue;
                                
                class ControllerClass {
                    public String methodWithDefaultedParameters(@DefaultValue("default-value") @RequestParam(defaultValue = "default-value", value = "q") String searchString) {
                        return "Hello";
                    }
                }
                """;

        testCopyAnnotationAttribute(sourceCode, expected);
    }

    @Test
    void givenTheMethodHasMultipleParameters_thenOnlyTheMethodParameterIsModifiedWhichContainsBothAnnotations() {
        String sourceCode = """                
                import org.springframework.web.bind.annotation.RequestParam;
                import org.springframework.web.bind.annotation.RequestHeader;
                import javax.ws.rs.DefaultValue;
                                
                class ControllerClass {
                    public String methodWithDefaultedParameters(
                        @DefaultValue("default-value-1") @RequestParam(value = "p1") String parameter1,
                        @RequestParam(value = "p2") String parameter2,
                        String parameter3,
                        @DefaultValue("default-value-4") @RequestHeader(value = "myOwnHeader") String myHeader,
                        @DefaultValue(value = "default-value-5") @RequestParam("p5") String parameter5
                    ) {
                        return "Hello";
                    }
                }
                """;

        String expected = """
                import org.springframework.web.bind.annotation.RequestParam;
                import org.springframework.web.bind.annotation.RequestHeader;
                import javax.ws.rs.DefaultValue;
                                
                class ControllerClass {
                    public String methodWithDefaultedParameters(
                        @DefaultValue("default-value-1") @RequestParam(defaultValue = "default-value-1", value = "p1") String parameter1,
                        @RequestParam(value = "p2") String parameter2,
                        String parameter3,
                        @DefaultValue("default-value-4") @RequestHeader(value = "myOwnHeader") String myHeader,
                        @DefaultValue(value = "default-value-5") @RequestParam(defaultValue = "default-value-5", value = "p5") String parameter5
                    ) {
                        return "Hello";
                    }
                }
                """;

        testCopyAnnotationAttribute(sourceCode, expected);
    }

    @Test
    void givenThereAreOtherAnnotationsPresentThanTheSourceAndTargetAnnotation_thenTheAttributeIsCopied() {
        String sourceCode = """                
                import org.springframework.web.bind.annotation.RequestParam;
                import javax.ws.rs.DefaultValue;
                import javax.validation.constraints.NotNull;
                                
                class ControllerClass {
                    public String methodWithDefaultedParameters(@RequestParam(value = "q") @NotNull @DefaultValue("default-value") String searchString) {
                        return "Hello";
                    }
                }
                """;

        String expected = """
                import org.springframework.web.bind.annotation.RequestParam;
                import javax.ws.rs.DefaultValue;
                import javax.validation.constraints.NotNull;
                                
                class ControllerClass {
                    public String methodWithDefaultedParameters(@RequestParam(defaultValue = "default-value", value = "q") @NotNull @DefaultValue("default-value") String searchString) {
                        return "Hello";
                    }
                }
                """;

        ProjectContext projectContext = TestProjectContext.buildProjectContext()
                .withJavaSources(sourceCode)
                .withBuildFileHavingDependencies(
                        "jakarta.ws.rs:jakarta.ws.rs-api:2.1.6",
                        "jakarta.validation:jakarta.validation-api:2.0.2",
                        "org.springframework:spring-web:" + SPRING_VERSION
                )
                .build();

        CopyAnnotationAttribute sut = new CopyAnnotationAttribute(
                "javax.ws.rs.DefaultValue", "value", "org.springframework.web.bind.annotation.RequestParam", "defaultValue");
        JavaSource javaSource = projectContext.getProjectJavaSources().list().get(0);
        javaSource.apply(sut);

        assertThat(javaSource.print()).isEqualTo(expected);
    }

    @Test
    void givenTheTargetAnnotationRelatesToAnotherMethodParameterThanTheSourceAnnotation_thenNoChangesAreMade() {
        String sourceCode = """                
                import org.springframework.web.bind.annotation.RequestParam;
                import javax.ws.rs.DefaultValue;
                                
                class ControllerClass {
                    public String methodWithParameters(
                        @RequestParam(value = "q") String parameter1,
                        @DefaultValue("default-value") String parameter2
                    ) {
                        return "Hello";
                    }
                }
                """;

        testCopyAnnotationAttribute(sourceCode, sourceCode);
    }

    @Test
    void givenOnlyTheTargetAnnotationIsPresent_thenNoChangesAreMade() {
        String sourceCode = """                
                import org.springframework.web.bind.annotation.RequestParam;
                import javax.ws.rs.DefaultValue;
                                
                class ControllerClass {
                    public String methodWithoutDefaultedParameters(@RequestParam(value = "q") String searchString) {
                        return "Hello";
                    }
                }
                """;

        testCopyAnnotationAttribute(sourceCode, sourceCode);
    }

    @Test
    void givenNoMethodParametersArePresent_thenNoChangesAreMade() {
        String sourceCode = """                
                import org.springframework.web.bind.annotation.RequestParam;
                import javax.ws.rs.DefaultValue;
                                
                class ControllerClass {
                    public String methodWithoutParameters() {
                        return "Hello";
                    }
                }
                """;

        testCopyAnnotationAttribute(sourceCode, sourceCode);
    }

    @Test
    void givenTheTargetAnnotationHasNoAttributesAndTheTargetAttributeIsNotValue_thenTheAttributeIsCopiedAsAssignment() {
        String sourceCode = """                
                import org.springframework.web.bind.annotation.RequestParam;
                import javax.ws.rs.DefaultValue;
                                
                class ControllerClass {
                    public String methodWithDefaultedParameters(@DefaultValue("default-value") @RequestParam String searchString) {
                        return "Hello";
                    }
                }
                """;

        String expected = """
                import org.springframework.web.bind.annotation.RequestParam;
                import javax.ws.rs.DefaultValue;
                                
                class ControllerClass {
                    public String methodWithDefaultedParameters(@DefaultValue("default-value") @RequestParam(defaultValue = "default-value") String searchString) {
                        return "Hello";
                    }
                }
                """;

        testCopyAnnotationAttribute(sourceCode, expected);
    }

    @Test
    void givenTheTargetAnnotationAlreadyHasAnAttributeWithTheTargetValue_thenNothingChanges() {
        String sourceCode = """                
                import org.springframework.web.bind.annotation.RequestParam;
                import javax.ws.rs.DefaultValue;
                                
                class ControllerClass {
                    public String methodWithDefaultedParameters(@DefaultValue("default-value") @RequestParam(defaultValue = "default-value") String searchString) {
                        return "Hello";
                    }
                }
                """;

        String expected = """
                import org.springframework.web.bind.annotation.RequestParam;
                import javax.ws.rs.DefaultValue;
                                
                class ControllerClass {
                    public String methodWithDefaultedParameters(@DefaultValue("default-value") @RequestParam(defaultValue = "default-value") String searchString) {
                        return "Hello";
                    }
                }
                """;

        testCopyAnnotationAttribute(sourceCode, expected);
    }

    @Test
    void givenTheTargetAnnotationAlreadyHasAnAttributeWithAnotherValue_thenTheValueOfTheTargetAttributeIsOverwritten() {
        String sourceCode = """                
                import org.springframework.web.bind.annotation.RequestParam;
                import javax.ws.rs.DefaultValue;
                                
                class ControllerClass {
                    public String methodWithDefaultedParameters(@DefaultValue("default-value") @RequestParam(defaultValue = "original-default-value") String searchString) {
                        return "Hello";
                    }
                }
                """;

        String expected = """
                import org.springframework.web.bind.annotation.RequestParam;
                import javax.ws.rs.DefaultValue;
                                
                class ControllerClass {
                    public String methodWithDefaultedParameters(@DefaultValue("default-value") @RequestParam(defaultValue = "default-value") String searchString) {
                        return "Hello";
                    }
                }
                """;

        testCopyAnnotationAttribute(sourceCode, expected);
    }

    @Test
    void givenTheTargetAnnotationHasNoAttributesAndTheTargetAttributeIsValue_thenTheAttributeIsCopiedAsLiteralValue() {
        String sourceCode = """                
                import org.springframework.web.bind.annotation.RequestParam;
                import javax.ws.rs.DefaultValue;
                                
                class ControllerClass {
                    public String methodWithDefaultedParameters(@DefaultValue("default-value") @RequestParam String searchString) {
                        return "Hello";
                    }
                }
                """;

        String expected = """
                import org.springframework.web.bind.annotation.RequestParam;
                import javax.ws.rs.DefaultValue;
                                
                class ControllerClass {
                    public String methodWithDefaultedParameters(@DefaultValue("default-value") @RequestParam("default-value") String searchString) {
                        return "Hello";
                    }
                }
                """;

        ProjectContext projectContext = TestProjectContext.buildProjectContext()
                .withJavaSources(sourceCode)
                .withBuildFileHavingDependencies(
                        "jakarta.ws.rs:jakarta.ws.rs-api:2.1.6",
                        "org.springframework:spring-web:" + SPRING_VERSION
                )
                .build();

        CopyAnnotationAttribute sut = new CopyAnnotationAttribute(
                "javax.ws.rs.DefaultValue", "value", "org.springframework.web.bind.annotation.RequestParam", "value");

        JavaSource javaSource = projectContext.getProjectJavaSources().list().get(0);
        javaSource.apply(sut);

        assertThat(javaSource.print()).isEqualTo(expected);
    }

    private static void testCopyAnnotationAttribute(String sourceCode, String expected) {
        ProjectContext projectContext = TestProjectContext.buildProjectContext()
                .withJavaSources(sourceCode)
                .withBuildFileHavingDependencies(
                        "jakarta.ws.rs:jakarta.ws.rs-api:2.1.6",
                        "org.springframework:spring-web:" + SPRING_VERSION
                )
                .build();

        CopyAnnotationAttribute sut = new CopyAnnotationAttribute(
                "javax.ws.rs.DefaultValue", "value", "org.springframework.web.bind.annotation.RequestParam", "defaultValue");

        JavaSource javaSource = projectContext.getProjectJavaSources().list().get(0);
        javaSource.apply(sut);

        assertThat(javaSource.print()).isEqualTo(expected);
    }
}
