package org.springframework.sbm.jee.jaxrs.recipes;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;
import org.springframework.sbm.jee.utils.AnnotationUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

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
        return new CopyAnnotationAttributeVisitor();
    }

    public class CopyAnnotationAttributeVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public @NotNull J.MethodDeclaration visitMethodDeclaration(@NotNull J.MethodDeclaration methodDeclaration, @NotNull ExecutionContext executionContext) {
            J.MethodDeclaration methodDecl = super.visitMethodDeclaration(methodDeclaration, executionContext);
            if (!hasMethodAtLeastOneParameterWithBothAnnotations(methodDecl)) {
                return methodDecl;
            }

            List<Statement> currentParameters = methodDecl.getParameters();
            AtomicBoolean argumentsChanged = new AtomicBoolean(false);
            List<Statement> newParameters = ListUtils.map(currentParameters, it -> {
                CopyAttributeValueResult copyAttributeValueResult = copyAttributeValueToTargetAnnotation(it, executionContext);
                if (copyAttributeValueResult.changed) {
                    argumentsChanged.set(true);
                }
                return copyAttributeValueResult.statement;
            });
            if (argumentsChanged.get()) {
                return methodDecl.withParameters(newParameters);
            }

            return methodDecl;
        }

        private CopyAttributeValueResult copyAttributeValueToTargetAnnotation(Statement statement, ExecutionContext executionContext) {
            if (!(statement instanceof J.VariableDeclarations methodParameterDeclaration) || methodParameterDeclaration.getLeadingAnnotations().size() < 2 || !hasBothAnnotations(methodParameterDeclaration)) {
                return new CopyAttributeValueResult(statement, false);
            }

            Optional<J.Literal> optionalSourceAttributeValue = getSourceAnnotationAttributeValue(methodParameterDeclaration);
            if (optionalSourceAttributeValue.isEmpty()) {
                return new CopyAttributeValueResult(statement, false);
            }

            J.Literal sourceAttributeValue = optionalSourceAttributeValue.get();
            if (sourceAttributeValue.getValue() == null) {
                return new CopyAttributeValueResult(statement, false);
            }

            List<J.Annotation> currentAnnotations = methodParameterDeclaration.getLeadingAnnotations();
            List<J.Annotation> newAnnotations = ListUtils.map(currentAnnotations, annotation -> {
                if (TypeUtils.isOfClassType(annotation.getType(), targetAnnotationType)) {
                    return AnnotationUtils.setAttribute(annotation, targetAttributeName, sourceAttributeValue, this, executionContext);
                }
                return annotation;
            });

            return new CopyAttributeValueResult(methodParameterDeclaration.withLeadingAnnotations(newAnnotations), true);
        }

        private Optional<J.Literal> getSourceAnnotationAttributeValue(J.VariableDeclarations methodParameterDeclaration) {
            return methodParameterDeclaration.getLeadingAnnotations().stream()
                    .filter(annotation -> TypeUtils.isOfClassType(annotation.getType(), sourceAnnotationType))
                    .flatMap(annotation -> AnnotationUtils.getAttributeValue(annotation, sourceAttributeName).stream())
                    .findAny();
        }

        private boolean hasBothAnnotations(J.VariableDeclarations methodParameterDeclaration) {
            return methodParameterContainsSourceAnnotationType(methodParameterDeclaration) &&
                    methodParameterContainsTargetAnnotationType(methodParameterDeclaration);
        }

        private boolean methodParameterContainsSourceAnnotationType(J.VariableDeclarations methodParameterDeclaration) {
            return methodParameterDeclaration.getLeadingAnnotations().stream().anyMatch(annotation -> TypeUtils.isOfClassType(annotation.getType(), sourceAnnotationType));
        }

        private boolean methodParameterContainsTargetAnnotationType(J.VariableDeclarations methodParameterDeclaration) {
            return methodParameterDeclaration.getLeadingAnnotations().stream().anyMatch(annotation -> TypeUtils.isOfClassType(annotation.getType(), targetAnnotationType));
        }

        private boolean hasMethodAtLeastOneParameterWithBothAnnotations(J.MethodDeclaration method) {
            if (CollectionUtils.isEmpty(method.getParameters())) {
                return false;
            }

            return method.getParameters().stream()
                    .filter(statement -> statement instanceof J.VariableDeclarations)
                    .map(statement -> (J.VariableDeclarations) statement)
                    .anyMatch(this::hasBothAnnotations);
        }
    }


    private record CopyAttributeValueResult(Statement statement, boolean changed) {
    }
}
