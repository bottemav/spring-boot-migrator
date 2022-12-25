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
package org.springframework.sbm.jee.jaxrs.recipes.visitors;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
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
public class CopyAnnotationAttributeVisitor extends JavaIsoVisitor<ExecutionContext> {
    String sourceAnnotationType;
    String sourceAttributeName;
    String targetAnnotationType;
    String targetAttributeName;

    @Override
    public @NotNull J.MethodDeclaration visitMethodDeclaration(@NotNull J.MethodDeclaration methodDeclaration, @NotNull ExecutionContext executionContext) {
        J.MethodDeclaration m = super.visitMethodDeclaration(methodDeclaration, executionContext);
        if (!hasMethodAtLeastOneParameterWithBothAnnotations(m)) {
            return m;
        }

        List<Statement> currentParameters = m.getParameters();
        AtomicBoolean argumentsChanged = new AtomicBoolean(false);
        List<Statement> newParameters = ListUtils.map(currentParameters, it -> {
            CopyAttributeValueResult copyAttributeValueResult = copyAttributeValueToTargetAnnotation(it, executionContext);
            if (copyAttributeValueResult.changed) {
                argumentsChanged.set(true);
            }
            return copyAttributeValueResult.statement;
        });
        if (argumentsChanged.get()) {
            return m.withParameters(newParameters);
        }

        return m;
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

    private record CopyAttributeValueResult(Statement statement, boolean changed) {
    }
}