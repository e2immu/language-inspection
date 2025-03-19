package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;

import java.util.Set;

/*
result of successful parsing.

computed from a Summary object

 */
public interface ParseResult {

    MethodInfo findMethod(String methodFqn);

    TypeInfo findType(String typeFqn);

    Set<TypeInfo> primaryTypes();

    TypeInfo firstType();

    int size();

    Set<TypeInfo> primaryTypesOfPackage(String packageName);
}
