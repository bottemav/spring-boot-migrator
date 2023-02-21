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
package org.springframework.sbm.java.migration.recipes;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.Identifier;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;
import org.springframework.sbm.java.impl.Utils;

import java.util.Map;
import java.util.function.Supplier;

public class FindReplaceFieldAccessors extends Recipe {
	
	final private String findFqName;
	final private String replaceFqName;
	final private Map<String, String> mappings;
	
	final private String simpleReplaceFqName;
	private final Supplier<JavaParser> javaParserSupplier;

	@Override
	public String getDisplayName() {
		return "Find field access for class " + findFqName + " and replace with field accesses from class " + replaceFqName;
	}
	
	@JsonCreator
	public FindReplaceFieldAccessors(Supplier<JavaParser> parserSupplier, String findFqName, String replaceFqName, Map<String, String> mappings) {
		this.findFqName = findFqName;
		this.replaceFqName = replaceFqName;
		this.mappings = mappings;
		this.simpleReplaceFqName = replaceFqName == null ? null :  Utils.getSimpleName(replaceFqName);
		javaParserSupplier = parserSupplier;
	}

	@Override
	protected TreeVisitor<?, ExecutionContext> getVisitor() {
		return new JavaVisitor<ExecutionContext>() {
			@Override
			public J visitIdentifier(Identifier ident, ExecutionContext executionContext) {
				J j = super.visitIdentifier(ident, executionContext);
				if (j instanceof J.Identifier id && id.getFieldType() != null && id.getFieldType().getOwner() != null && findFqName.equals(id.getFieldType().getOwner().toString()) && id.getType() != null && id.getType().toString().equals("java.lang.String")) {
					String newSimpleName = mappings.get(id.getSimpleName());
					if (newSimpleName != null) {
						return id.withSimpleName(newSimpleName)
								.withFieldType(id.getFieldType()
										.withName(newSimpleName)
										.withOwner(JavaType.buildType(replaceFqName + "." + newSimpleName))
								);
					}
				}
				return j;
			}

			@Override
			public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
				J.FieldAccess fa = fieldAccess;
				Expression target = fa.getTarget();
				JavaType.FullyQualified asClass = TypeUtils.asFullyQualified(target.getType());

				String replaceField = mappings.get(fa.getName().getSimpleName());
				if (asClass != null && asClass.getFullyQualifiedName().equals(findFqName) &&
						replaceField != null) {
					maybeRemoveImportAndParentTypeImports(findFqName);
					maybeAddImport(replaceFqName);
					JavaType replaceType = JavaType.buildType(replaceFqName);
					if (fa.getTarget() instanceof Identifier) {
						fa = fa
								.withName(fa.getName().withSimpleName(replaceField).withType(replaceType))
								.withType(replaceType)
								.withTarget(new Identifier(target.getId(), target.getPrefix(), target.getMarkers(), simpleReplaceFqName, replaceType, null));
					} else if (fa.getTarget() instanceof J.FieldAccess) {
						fa = fa
								.withName(fa.getName().withSimpleName(replaceField).withType(replaceType))
								.withType(replaceType)
								.withTarget(TypeTree.build(replaceFqName));
					}
				}
				return super.visitFieldAccess(fa, ctx);
			}

			private void maybeRemoveImportAndParentTypeImports(String fqName) {
				int idx = fqName.lastIndexOf('.');
				String simpleName = idx > 0 && idx < fqName.length() - 1 ? fqName.substring(idx + 1) : fqName;
				if (Character.isUpperCase(simpleName.charAt(0))) {
					maybeRemoveImport(fqName);
					maybeRemoveImportAndParentTypeImports(fqName.substring(0, idx));
				}
			}

		};
	}


}
