package com.acme.jaxrs.rest;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.Collections;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;

@Path("companies")
@Component
public class CompanyResource {
    @GET
    @Produces(APPLICATION_JSON)
    @Path("/list")
    public List<String> listCompanies(
            @QueryParam("q") String searchBy,
            @QueryParam("sort") String sort,
            @DefaultValue("0") @QueryParam("page") int page,
            @DefaultValue("10") @QueryParam("size") int pageSize) {
        return Collections.emptyList();
    }

    @POST
    @Path("{companyId}/logo")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_JSON)
    public String uploadCompanyLogo(@PathParam("companyId") String companyId,
                                            @FormDataParam("logo") FormDataBodyPart file) {
        return null;
    }

}
