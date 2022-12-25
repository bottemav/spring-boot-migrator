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
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RemoveAnnotation;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemoveAnnotationIfAccompaniedVisitorV2 extends JavaIsoVisitor<ExecutionContext> {
    private static final String ANNOTATION_REMOVED_KEY = "annotationRemoved";
    String annotationTypeToRemove;
    String additionalAnnotationType;

    @Override
    public J.Annotation visitAnnotation(@NotNull J.Annotation annotation, @NotNull ExecutionContext ctx) {
        J.Annotation a = super.visitAnnotation(annotation, ctx);

        if (!TypeUtils.isOfClassType(a.getType(), annotationTypeToRemove)) {
            return a;
        }

        Cursor parent = getCursor().getParent();
        if (parent == null) {
            return a;
        }
        J.VariableDeclarations variableDeclaration = parent.getValue();
        if (variableDeclarationContainsAnnotationType(variableDeclaration, additionalAnnotationType)) {
            JavaIsoVisitor<ExecutionContext> removeAnnotationVisitor = new RemoveAnnotation("@" + annotationTypeToRemove)
                    .getVisitor();
            return (J.Annotation) removeAnnotationVisitor.visit(a, ctx, getCursor());
        }

        return a;
    }

    private boolean isAnnotationToRemoveAndAdditionalAnnotationIsPresent(J.Annotation annotation) {
        return TypeUtils.isOfClassType(annotation.getType(), annotationTypeToRemove)
                && this.getCursor().getParentOrThrow().getValue() instanceof J.VariableDeclarations v
                && variableDeclarationContainsAnnotationType(v, additionalAnnotationType);
    }

    private boolean variableDeclarationContainsAnnotationType(J.VariableDeclarations variableDeclaration, String annotationType) {
        return variableDeclaration.getLeadingAnnotations().stream().anyMatch(annotation -> TypeUtils.isOfClassType(annotation.getType(), annotationType));
    }

    private List<J.Annotation> removeAnnotationOrEmpty(List<J.Annotation> leadingAnnotations, J.Annotation targetAnnotation) {
        int index = leadingAnnotations.indexOf(targetAnnotation);
        List<J.Annotation> newLeadingAnnotations = new ArrayList<>();
        if (index == 0) {
            J.Annotation nextAnnotation = leadingAnnotations.get(1);
            if (!nextAnnotation.getPrefix().equals(targetAnnotation.getPrefix())) {
                newLeadingAnnotations.add(nextAnnotation.withPrefix(targetAnnotation.getPrefix()));

                for(int i = 2; i < leadingAnnotations.size(); ++i) {
                    newLeadingAnnotations.add(leadingAnnotations.get(i));
                }
            }
        }

        return newLeadingAnnotations;
    }
}
