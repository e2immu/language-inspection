package org.e2immu.language.inspection.api.parser;

import org.e2immu.annotation.NotNull;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.ImportStatement;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.inspection.api.resource.TypeMap;

/*
NOT to be used by byte code inspection: exclusive to parser system!
 */
public interface TypeContext {
    void addToImportMap(ImportStatement importStatement);

    // including new import map
    TypeContext newCompilationUnit(TypeMap.Builder typeMap, CompilationUnit compilationUnit);

    ImportMap importMap();

    CompilationUnit compilationUnit();

    // name can be fully qualified
    NamedType get(String name, boolean complain);

    void addToContext(@NotNull NamedType namedType);

    void addToContext(@NotNull NamedType namedType, boolean allowOverwrite);

    void addToContext(String altName, @NotNull NamedType namedType, boolean allowOverwrite);

    TypeContext newTypeContext();
}
