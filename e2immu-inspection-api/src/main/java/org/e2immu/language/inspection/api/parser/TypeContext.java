package org.e2immu.language.inspection.api.parser;

import org.e2immu.annotation.NotNull;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.ImportStatement;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.List;

/*
NOT to be used by byte code inspection: exclusive to parser system!
 */
public interface TypeContext {
    /*
    return true when all types in the hierarchy have been resolved
     */
    boolean addSubTypesOfHierarchyReturnAllDefined(TypeInfo typeInfo);

    /*
    return true when all types in the hierarchy of a static * import have been resolved,
    or this was not a * import.
     */
    boolean addToStaticImportMap(CompilationUnit currentCompilationUnit, ImportStatement importStatement);

    void addNonStaticImportToContext(ImportStatement importStatement);

    Variable findStaticFieldImport(String name);

    TypeContext newCompilationUnit(CompilationUnit compilationUnit);

    TypeContext newAnonymousClassBody(TypeInfo baseType);

    StaticImportMap importMap();

    CompilationUnit compilationUnit();

    // a.b.X.I.J -> X, I, J
    // X.I.J -> X, I, J
    // J -> J
    // I.J -> I, J,
    List<? extends NamedType> getWithQualification(String name, boolean complain);

    void addToContext(@NotNull NamedType namedType);

    void addToContext(@NotNull NamedType namedType, boolean allowOverwrite);

    void addToContext(String altName, @NotNull NamedType namedType, boolean allowOverwrite);

    TypeContext newTypeContext();

    List<TypeInfo> typesInSamePackage(String packageName);
}
