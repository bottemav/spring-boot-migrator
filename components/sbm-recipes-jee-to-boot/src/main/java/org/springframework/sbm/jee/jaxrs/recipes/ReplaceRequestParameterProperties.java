package org.springframework.sbm.jee.jaxrs.recipes;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Recipe;

public class ReplaceRequestParameterProperties extends Recipe {
    public ReplaceRequestParameterProperties() {
        doNext(new CopyAnnotationAttribute(
                "javax.ws.rs.DefaultValue", "value", "org.springframework.web.bind.annotation.RequestParam", "defaultValue")
        );
        doNext(new RemoveAnnotationIfAccompanied(
                "javax.ws.rs.DefaultValue", "org.springframework.web.bind.annotation.RequestParam"
        ));
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Migrate the properties of a request parameter: default value, ...";
    }
}
