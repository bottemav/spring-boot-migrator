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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemoveAnnotationIfAccompaniedVisitor extends JavaIsoVisitor<ExecutionContext> {
    /* This class if based on org.openrewrite.java.RemoveAnnotationVisitor
            but with a different implementation of visitAnnotation and other fields
            */
    private  static final String ANNOTATION_REMOVED_KEY = "annotationRemoved";
    String annotationTypeToRemove;
    String additionalAnnotationType;

    @Override
    public J.ClassDeclaration visitClassDeclaration(@NotNull J.ClassDeclaration classDecl, @NotNull ExecutionContext ctx) {
        J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
        J.Annotation annotationRemoved = (J.Annotation)this.getCursor().pollMessage(ANNOTATION_REMOVED_KEY);
        List<J.Annotation> leadingAnnotations = classDecl.getLeadingAnnotations();
        if (annotationRemoved != null && !leadingAnnotations.isEmpty()) {
            if (leadingAnnotations.get(0) == annotationRemoved && leadingAnnotations.size() == 1) {
                if (!c.getModifiers().isEmpty()) {
                    c = c.withModifiers(Space.formatFirstPrefix(c.getModifiers(), Space.firstPrefix(c.getModifiers()).withWhitespace("")));
                } else if (c.getPadding().getTypeParameters() != null) {
                    c = c.getPadding().withTypeParameters(c.getPadding().getTypeParameters().withBefore(c.getPadding().getTypeParameters().getBefore().withWhitespace("")));
                } else {
                    c = c.getAnnotations().withKind(c.getAnnotations().getKind().withPrefix(c.getAnnotations().getKind().getPrefix().withWhitespace("")));
                }
            } else {
                List<J.Annotation> newLeadingAnnotations = this.removeAnnotationOrEmpty(leadingAnnotations, annotationRemoved);
                if (!newLeadingAnnotations.isEmpty()) {
                    c = c.withLeadingAnnotations(newLeadingAnnotations);
                }
            }
        }

        return c;
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(@NotNull J.MethodDeclaration method, @NotNull ExecutionContext ctx) {
        J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
        J.Annotation annotationRemoved = (J.Annotation)this.getCursor().pollMessage(ANNOTATION_REMOVED_KEY);
        List<J.Annotation> leadingAnnotations = method.getLeadingAnnotations();
        if (annotationRemoved != null && !leadingAnnotations.isEmpty()) {
            if (leadingAnnotations.get(0) == annotationRemoved && leadingAnnotations.size() == 1) {
                if (!m.getModifiers().isEmpty()) {
                    m = m.withModifiers(Space.formatFirstPrefix(m.getModifiers(), Space.firstPrefix(m.getModifiers()).withWhitespace("")));
                } else if (m.getPadding().getTypeParameters() != null) {
                    m = m.getPadding().withTypeParameters(m.getPadding().getTypeParameters().withPrefix(m.getPadding().getTypeParameters().getPrefix().withWhitespace("")));
                } else if (m.getReturnTypeExpression() != null) {
                    m = m.withReturnTypeExpression((TypeTree)m.getReturnTypeExpression().withPrefix(m.getReturnTypeExpression().getPrefix().withWhitespace("")));
                } else {
                    m = m.withName(m.getName().withPrefix(m.getName().getPrefix().withWhitespace("")));
                }
            } else {
                List<J.Annotation> newLeadingAnnotations = this.removeAnnotationOrEmpty(leadingAnnotations, annotationRemoved);
                if (!newLeadingAnnotations.isEmpty()) {
                    m = m.withLeadingAnnotations(newLeadingAnnotations);
                }
            }
        }

        return m;
    }

    @Override
    public J.VariableDeclarations visitVariableDeclarations(@NotNull J.VariableDeclarations multiVariable, @NotNull ExecutionContext ctx) {
        J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, ctx);
        J.Annotation annotationRemoved = (J.Annotation)this.getCursor().pollMessage(ANNOTATION_REMOVED_KEY);
        List<J.Annotation> leadingAnnotations = multiVariable.getLeadingAnnotations();
        if (annotationRemoved != null && !leadingAnnotations.isEmpty()) {
            if (leadingAnnotations.get(0) == annotationRemoved && leadingAnnotations.size() == 1) {
                if (!v.getModifiers().isEmpty()) {
                    v = v.withModifiers(Space.formatFirstPrefix(v.getModifiers(), Space.firstPrefix(v.getModifiers()).withWhitespace("")));
                } else if (v.getTypeExpression() != null) {
                    v = v.withTypeExpression((TypeTree)v.getTypeExpression().withPrefix(v.getTypeExpression().getPrefix().withWhitespace("")));
                }
            } else {
                List<J.Annotation> newLeadingAnnotations = this.removeAnnotationOrEmpty(leadingAnnotations, annotationRemoved);
                if (!newLeadingAnnotations.isEmpty()) {
                    v = v.withLeadingAnnotations(newLeadingAnnotations);
                }
            }
        }

        return v;
    }

    @Override
    public J.Annotation visitAnnotation(@NotNull J.Annotation annotation, @NotNull ExecutionContext ctx) {
        if (isAnnotationToRemoveAndAdditionalAnnotationIsPresent(annotation)) {
            getCursor().getParentOrThrow().putMessage(ANNOTATION_REMOVED_KEY, annotation);
            maybeRemoveImport(TypeUtils.asFullyQualified(annotation.getType()));
            //noinspection ConstantConditions
            return null;
        }
        return super.visitAnnotation(annotation, ctx);
    }

    private boolean isAnnotationToRemoveAndAdditionalAnnotationIsPresent(J.Annotation annotation) {
        return TypeUtils.isOfClassType(annotation.getType(), annotationTypeToRemove)
                && this.getCursor().getParentOrThrow().getValue() instanceof J.VariableDeclarations v
                && variableDeclarationContainsAnnotationType(v, additionalAnnotationType);
    }

    private boolean variableDeclarationContainsAnnotationType(J.VariableDeclarations methodParameterDeclaration, String annotationType) {
        return methodParameterDeclaration.getLeadingAnnotations().stream().anyMatch(annotation -> TypeUtils.isOfClassType(annotation.getType(), annotationType));
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
