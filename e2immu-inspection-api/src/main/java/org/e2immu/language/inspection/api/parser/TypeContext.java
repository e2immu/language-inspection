package org.e2immu.language.inspection.api.parser;

import org.e2immu.annotation.NotNull;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.ImportStatement;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.inspection.api.resource.TypeMap;

public interface TypeContext {
    void addToImportMap(ImportStatement importStatement);

    TypeMap.Builder typeMap();

    ImportMap importMap();

    CompilationUnit compilationUnit();

    TypeInfo getFullyQualified(Class<?> clazz);

    TypeInfo getFullyQualified(String fullyQualifiedName, boolean complain);

    NamedType get(String name, boolean complain);

    boolean isKnown(String fullyQualified);

    void addToContext(@NotNull NamedType namedType);

    void addToContext(@NotNull NamedType namedType, boolean allowOverwrite);

    void addToContext(String altName, @NotNull NamedType namedType, boolean allowOverwrite);

    TypeContext newTypeContext();
}
