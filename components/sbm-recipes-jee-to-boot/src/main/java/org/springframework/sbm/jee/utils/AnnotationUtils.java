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
package org.springframework.sbm.jee.utils;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AnnotationUtils {
    public static final String VALUE_ATTRIBUTE_NAME = "value";

    private AnnotationUtils() {
    }

    public static Optional<J.Literal> getAttributeValue(J.Annotation annotation, String attributeName) {
        if (CollectionUtils.isEmpty(annotation.getArguments())) {
            return Optional.empty();
        }

        for (Expression argument : annotation.getArguments()) {
            if (argument instanceof J.Assignment as) {
                J.Identifier variable = (J.Identifier) as.getVariable();
                if (variable.getSimpleName().equals(attributeName)) {
                    return Optional.of((J.Literal) as.getAssignment());
                }
            } else if (argument instanceof J.Literal literal && VALUE_ATTRIBUTE_NAME.equals(attributeName)) {
                return Optional.of(literal);
            }
        }

        return Optional.empty();
    }

    public static J.Annotation setAttribute(J.Annotation annotation, String attributeName, J.Literal attributeValue, JavaIsoVisitor<ExecutionContext> javaIsoVisitor, ExecutionContext executionContext) {
        List<Expression> currentArguments = annotation.getArguments();
        if (CollectionUtils.isEmpty(currentArguments)) {
            return setAttributeOnAnnotationWithoutArguments(annotation, attributeName, attributeValue, javaIsoVisitor);
        }

        List<Expression> expandedArguments = currentArguments;
        AtomicBoolean foundTargetAttribute = new AtomicBoolean(false);
        if (currentArguments.size() == 1 && currentArguments.get(0) instanceof J.Literal literalArgument) {
            if (VALUE_ATTRIBUTE_NAME.equals(attributeName)) {
                return annotation.withTemplate(
                        JavaTemplate.builder(javaIsoVisitor::getCursor, "#{}").build(),
                        annotation.getCoordinates().replaceArguments(),
                        attributeValue);
            }

            expandedArguments = List.of(
                    ((J.Annotation) annotation.withTemplate(
                            JavaTemplate.builder(javaIsoVisitor::getCursor, "value = #{}")
                                    .build(),
                            annotation.getCoordinates().replaceArguments(),
                            literalArgument)).getArguments().get(0)
            );
        } else {
            AtomicBoolean targetAttributeHasTargetValue = new AtomicBoolean(false);
            List<Expression> newArgs = ListUtils.map(expandedArguments, it -> {
                if (it instanceof J.Assignment as) {
                    J.Identifier variable = (J.Identifier) as.getVariable();
                    if (!attributeName.equals(variable.getSimpleName())) {
                        return it;
                    }
                    J.Literal value = (J.Literal) as.getAssignment();
                    foundTargetAttribute.set(true);
                    if (attributeValue.equals(value)) {
                        targetAttributeHasTargetValue.set(true);
                        return it;
                    }
                    return as.withAssignment(value.withValue(attributeValue).withValueSource(attributeValue.getValueSource()));
                }
                return it;
            });
            if (foundTargetAttribute.get()) {
                if (targetAttributeHasTargetValue.get()) {
                    return annotation;
                }

                return annotation.withArguments(newArgs);
            }
        }

        // The target attribute name is not in the original attributes of the target annotation, so add it
        return addAttributeToAnnotation(annotation, attributeName, attributeValue, javaIsoVisitor, executionContext, expandedArguments);
    }

    @NotNull
    private static J.Annotation addAttributeToAnnotation(J.Annotation annotation, String targetAttributeName, J.Literal sourceAttributeValue, JavaVisitor<ExecutionContext> javaVisitor, ExecutionContext executionContext, List<Expression> expandedArguments) {
        J.Assignment as = (J.Assignment) ((J.Annotation) annotation.withTemplate(
                JavaTemplate.builder(javaVisitor::getCursor, targetAttributeName + " = #{}")
                        .build(),
                annotation.getCoordinates().replaceArguments(),
                sourceAttributeValue)).getArguments().get(0);
        List<Expression> newArguments = ListUtils.concat(as, expandedArguments);
        return javaVisitor.autoFormat(annotation.withArguments(newArguments), executionContext);
    }

    @NotNull
    private static J.Annotation setAttributeOnAnnotationWithoutArguments(J.Annotation annotation, String targetAttributeName, J.Literal sourceAttributeValue, JavaVisitor<ExecutionContext> javaVisitor) {
        String templateCode = VALUE_ATTRIBUTE_NAME.equals(targetAttributeName) ? "#{}" : targetAttributeName + " = #{}";
        return annotation.withTemplate(
                JavaTemplate.builder(javaVisitor::getCursor, templateCode).build(),
                annotation.getCoordinates().replaceArguments(),
                sourceAttributeValue);
    }
}
