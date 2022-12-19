package org.springframework.sbm.jee.jaxrs.recipes;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesType;
import org.springframework.sbm.jee.jaxrs.recipes.visitors.CopyAnnotationAttributeVisitor;

@Value
@EqualsAndHashCode(callSuper = true)
public class CopyAnnotationAttribute extends Recipe {
    @Option(displayName = "Source Annotation Type",
            description = "The fully qualified name of the source annotation.",
            example = "org.junit.Test")
    String sourceAnnotationType;

    @Option(displayName = "Source Attribute name",
            description = "The name of the attribute of the source annotation containing the value to copy",
            required = false,
            example = "timeout")
    String sourceAttributeName;

    @Option(displayName = "Target Annotation Type",
            description = "The fully qualified name of the target annotation.",
            example = "org.junit.Test")
    String targetAnnotationType;

    @Option(displayName = "Target Attribute name",
            description = "The name of the attribute of the target annotation which must be set to the value of the source attribute.",
            required = false,
            example = "timeout")
    String targetAttributeName;

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesType<>(sourceAnnotationType);
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Copy an annotation attribute on a method parameter";
    }

    @Override
    public @NotNull String getDescription() {
        return "Copy the value of an annotation attribute on a method parameter to another annotation attribute.";
    }

    @Override
    protected @NotNull JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new CopyAnnotationAttributeVisitor(
                sourceAnnotationType,
                sourceAttributeName,
                targetAnnotationType,
                targetAttributeName
        );
    }
}
