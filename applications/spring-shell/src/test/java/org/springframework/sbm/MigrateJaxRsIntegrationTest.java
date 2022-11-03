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
                
                import org.glassfish.jersey.media.multipart.FormDataBodyPart;
                import org.glassfish.jersey.media.multipart.FormDataParam;
                import org.springframework.stereotype.Component;
                import org.springframework.web.bind.annotation.*;
                
                import javax.ws.rs.DefaultValue;
                import java.util.Collections;
                import java.util.List;
                
                import static org.springframework.http.MediaType.APPLICATION_JSON;
                import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
                
                
                @Component
                @RestController
                @RequestMapping(value = "companies")
                public class CompanyResource {
                    @RequestMapping(produces = APPLICATION_JSON, value = "/list", method = RequestMethod.GET)
                    public List<String> listCompanies(
                            @RequestParam(value = "q") String searchBy,
                            @RequestParam(value = "sort") String sort,
                            @DefaultValue("0") @RequestParam(value = "page") int page,
                            @DefaultValue("10") @RequestParam(value = "size") int pageSize) {
                        return Collections.emptyList();
                    }
                
                    @RequestMapping(value = "{companyId}/logo", consumes = MULTIPART_FORM_DATA, produces = APPLICATION_JSON, method = RequestMethod.POST)
                    public String uploadCompanyLogo(@PathVariable("companyId") String companyId,
                            @FormDataParam("logo")@RequestBody FormDataBodyPart file) {
                        return null;
                    }
                
                }
                """
        );
    }
}
