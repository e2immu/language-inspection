package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.Map;

public interface Context {
    VariableContext dependentVariableContext();

    ForwardType emptyForwardType();

    FieldInfo enclosingField();

    MethodInfo enclosingMethod();

    TypeInfo enclosingType();

    ForwardType erasureForwardType();

    Info info();

    MethodResolution methodResolution();

    Context newCompilationUnit(CompilationUnit compilationUnit);

    Context newLambdaContext(MethodInfo sam);

    ForwardType newForwardType(ParameterizedType parameterizedType);

    ForwardType newForwardType(ParameterizedType parameterizedType,
                               boolean erasure, Map<NamedType,
            ParameterizedType> typeParameterMap);

    Context newAnonymousClassBody(TypeInfo anonymousType);

    Context newSubType(TypeInfo typeInfo);

    Context newTypeBody();

    Context newTypeContext();

    Context newVariableContext(String reason, VariableContext variableContext);

    Context newVariableContext(String lambda);

    Context newVariableContextForMethodBlock(MethodInfo methodInfo, ForwardType forwardType);

    Resolver resolver();

    Runtime runtime();

    TypeContext typeContext();

    VariableContext variableContext();

    Summary summary();

    ParseHelper parseHelper();

    GenericsHelper genericsHelper();

    DetailedSources.Builder newDetailedSourcesBuilder();
}
