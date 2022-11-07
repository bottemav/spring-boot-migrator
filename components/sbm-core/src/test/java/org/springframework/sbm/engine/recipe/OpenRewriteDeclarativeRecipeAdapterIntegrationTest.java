/*
 * Copyright 2021 - 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.sbm.engine.recipe;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.sbm.engine.context.ProjectContext;
import org.springframework.sbm.java.migration.conditions.HasAnnotation;
import org.springframework.sbm.project.RewriteSourceFileWrapper;
import org.springframework.sbm.project.resource.ResourceHelper;
import org.springframework.sbm.project.resource.TestProjectContext;
import org.springframework.validation.beanvalidation.CustomValidatorBean;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        RecipeParser.class,
        RewriteRecipeLoader.class,
        YamlObjectMapperConfiguration.class,
        CustomValidator.class,
        ResourceHelper.class,
        ActionDeserializerRegistry.class,
        DefaultActionDeserializer.class,
        RewriteMigrationResultMerger.class,
        RewriteRecipeLoader.class,
        RewriteRecipeRunner.class,
        RewriteSourceFileWrapper.class,
        CustomValidatorBean.class
})
class OpenRewriteDeclarativeRecipeAdapterIntegrationTest {

    @Autowired
    RecipeParser recipeParser;
    @Autowired
    private RewriteRecipeLoader rewriteRecipeLoader;
    @Autowired
    private RewriteRecipeRunner rewriteRecipeRunner;

    @Test
    void adapterActionShouldExecuteOpenRewriteBuilderRecipeModifyingSpringAnnotation() {
        OpenRewriteDeclarativeRecipeAdapter recipeAdapter = OpenRewriteDeclarativeRecipeAdapter.builder()
                .condition(HasAnnotation.builder().annotation("org.springframework.web.bind.annotation.RequestParam").build())
                .description("Adds required=false to all @RequestParam annotations")
                .rewriteRecipeLoader(rewriteRecipeLoader)
                .rewriteRecipeRunner(rewriteRecipeRunner)
                .openRewriteRecipe(
                        """
                                type: specs.openrewrite.org/v1beta/recipe
                                name: org.springframework.sbm.jee.MakeRequestParamsOptional
                                displayName: Set required=false for @RequestParam without 'required'
                                description: Set required=false for @RequestParam without 'required'
                                recipeList:
                                  - org.openrewrite.java.AddOrUpdateAnnotationAttribute:
                                      annotationType: "org.springframework.web.bind.annotation.RequestParam"
                                      attributeName: "required"
                                      attributeValue: "false"
                                      addOnly: true
                                """)
                .build();

        // create context
        String javaSource = """
                import org.springframework.stereotype.Controller;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestParam;
                                
                @Controller
                public class HelloController {
                    @GetMapping("/")
                    public String sayHello(@RequestParam String name) {
                        return "Hello " + name;
                    }
                }
                """;

        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xmlns="http://maven.apache.org/POM/4.0.0"
                  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.springframework.samples</groupId>
                  <artifactId>spring-petclinic</artifactId>
                  <version>1.5.1</version>
                                
                  <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>2.6.4</version>
                  </parent>
                  <name>demo</name>
                                
                  <properties>
                                
                    <!-- Generic properties -->
                    <java.version>11</java.version>
                  </properties>
                  <dependencies>
                    <dependency>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-web</artifactId>
                    </dependency>
                  </dependencies>
                </project>
                """;

        ProjectContext context = TestProjectContext.buildProjectContext()
                .addJavaSource("src/main/java", javaSource)
                .addProjectResource("pom.xml", pom)
                .build();
        // and apply the adapter
        recipeAdapter.apply(context);
        // verify the openrewrite recipe ran
        assertThat(context.getProjectJavaSources().list().get(0).print()).isEqualTo(
                """
                        import org.springframework.stereotype.Controller;
                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.RequestParam;
                                        
                        @Controller
                        public class HelloController {
                            @GetMapping("/")
                            public String sayHello(@RequestParam(required = false) String name) {
                                return "Hello " + name;
                            }
                        }
                        """
        );
    }

    @Test
    void adapterActionShouldExecuteOpenRewriteYamlRecipe() throws IOException {
        String validSbmRecipeYaml =
                        "- name: test-recipe\n" +
                        "  description: Replace deprecated spring.datasource.* properties\n" +
                        "  condition:\n" +
                        "    type: org.springframework.sbm.common.migration.conditions.TrueCondition\n" +
                        "  actions:\n" +
                        "    - type: org.springframework.sbm.engine.recipe.OpenRewriteDeclarativeRecipeAdapter\n" +
                        "      condition:\n" +
                        "        type: org.springframework.sbm.common.migration.conditions.TrueCondition\n" +
                        "      description: Call a OpenRewrite recipe\n" +
                        "      openRewriteRecipe: |-\n" +
                        "        type: specs.openrewrite.org/v1beta/recipe\n" +
                        "        name: org.openrewrite.java.RemoveAnnotation\n" +
                        "        displayName: Order imports\n" +
                        "        description: Order imports\n" +
                        "        recipeList:\n" +
                        "          - org.openrewrite.java.RemoveAnnotation:\n" +
                        "              annotationPattern: \"@java.lang.Deprecated\"\n" +
                        "          - org.openrewrite.java.format.AutoFormat";

        // parse the recipe
        Recipe[] recipes = recipeParser.parseRecipe(validSbmRecipeYaml);
        assertThat(recipes[0].getActions().get(0)).isInstanceOf(OpenRewriteDeclarativeRecipeAdapter.class);
        // retrieve adapter action
        OpenRewriteDeclarativeRecipeAdapter recipeAdapter = (OpenRewriteDeclarativeRecipeAdapter) recipes[0].getActions().get(0);
        // create a test prokect
        String javaSource = "@java.lang.Deprecated\n" +
                "public class Foo {}";
        ProjectContext context = TestProjectContext.buildProjectContext()
                .addJavaSource("src/main/java", javaSource)
                .build();
        // run the adapter action and thus the declared rewrite recipes
        recipeAdapter.apply(context);
        // verify that the rewrite recipes were executed (reformatted, @Deprecated added)
        assertThat(context.getProjectJavaSources().list().get(0).print()).isEqualTo(
                "public class Foo {\n" +
                        "}"
        );
    }
}
