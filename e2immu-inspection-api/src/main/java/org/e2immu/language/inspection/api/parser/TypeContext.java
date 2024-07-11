package org.e2immu.language.inspection.api.parser;

import org.e2immu.annotation.NotNull;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.ImportStatement;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.variable.FieldReference;

import java.util.List;
import java.util.Map;

/*
NOT to be used by byte code inspection: exclusive to parser system!
 */
public interface TypeContext {
    void addToImportMap(ImportStatement importStatement);

    TypeContext newCompilationUnit(CompilationUnit compilationUnit);

    Map<String, FieldReference> staticFieldImports(Runtime runtime);

    TypeContext newAnonymousClassBody(TypeInfo baseType);

    ImportMap importMap();

    CompilationUnit compilationUnit();

    // name can be fully qualified
    NamedType get(String name, boolean complain);

    void addToContext(@NotNull NamedType namedType);

    void addToContext(@NotNull NamedType namedType, boolean allowOverwrite);

    void addToContext(String altName, @NotNull NamedType namedType, boolean allowOverwrite);

    TypeContext newTypeContext();

    List<TypeInfo> typesInSamePackage(String packageName);
}
