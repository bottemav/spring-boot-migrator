package org.springframework.sbm;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MigrateJaxRsIntegrationTest extends IntegrationTestBaseClass {

    @Override
    protected String getTestSubDir() {
        return "boot-migration-jax-rs";
    }

    @Test
    @Tag("integration")
    void migrateJaxRsApplication() {
        intializeTestProject();
        executeMavenGoals(getTestDir(), "clean", "package");
        scanProject();
        assertApplicableRecipesContain(
                "migrate-jax-rs",
                "cn-spring-cloud-config-server"
        );

        applyRecipe(
                "migrate-jax-rs"
        );

        String movies = loadJavaFile("com.acme.jaxrs.rest", "CompanyResource");
        assertThat(movies).contains(
                """
                        package com.acme.jaxrs.rest;
                                        
                        import org.springframework.stereotype.Component;
                        import org.springframework.web.bind.annotation.RequestMapping;
                        import org.springframework.web.bind.annotation.RequestMethod;
                        import org.springframework.web.bind.annotation.RequestParam;
                        import org.springframework.web.bind.annotation.RestController;
                                        
                        import java.util.Collections;
                        import java.util.List;
                                        
                        import static org.springframework.http.MediaType.APPLICATION_JSON;
                                        
                                        
                        @Component
                        @RestController
                        @RequestMapping(value = "companies")
                        public class CompanyResource {
                            @RequestMapping(produces = APPLICATION_JSON, value = "/list", method = RequestMethod.GET)
                            public List<String> listCompanies(
                                    @RequestParam(value = "q", required = false) String searchBy,
                                    @RequestParam(value = "sort", required = false) String sort) {
                                return Collections.emptyList();
                            }
                                        
                        }
                        """
        );
    }
}
