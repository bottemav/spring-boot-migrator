package org.springframework.sbm.jee.jaxrs.recipes;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.search.UsesType;
import org.springframework.sbm.jee.jaxrs.recipes.visitors.RemoveAnnotationIfAccompaniedVisitor;

@EqualsAndHashCode(callSuper = true)
@Value
public class RemoveAnnotationIfAccompanied extends Recipe {
    @Option(displayName = "Annotation Type to remove",
            description = "The fully qualified name of the annotation to remove.",
            example = "org.junit.Test")
    String annotationTypeToRemove;

    @Option(displayName = "Annotation Type which must also be present",
            description = "The fully qualified name of the annotation that must also be present.",
            example = "org.junit.Test")
    String additionalAnnotationType;

    @Override
    public @NotNull String getDisplayName() {
        return "Remove annotation if method parameter also has the other annotation";
    }

    @Override
    public @NotNull String getDescription() {
        return "Remove matching annotation if the method parameter also has the other annotation.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesType<>(annotationTypeToRemove);
    }

    @Override
    public @NotNull RemoveAnnotationIfAccompaniedVisitor getVisitor() {
        return new RemoveAnnotationIfAccompaniedVisitor(annotationTypeToRemove, additionalAnnotationType);
    }
}
