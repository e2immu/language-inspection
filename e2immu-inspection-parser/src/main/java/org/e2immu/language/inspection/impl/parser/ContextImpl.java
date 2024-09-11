package org.e2immu.language.inspection.impl.parser;

import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;

import org.e2immu.language.inspection.api.parser.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


public class ContextImpl implements Context {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContextImpl.class);

    private record Data(Runtime runtime, Summary summary, MethodResolution methodResolution) {
    }

    private final Data data;
    private final TypeInfo enclosingType;
    private final MethodInfo enclosingMethod;
    private final FieldInfo enclosingField;
    private final Resolver resolver;
    private final TypeContext typeContext;
    private final VariableContext variableContext;
    private final ForwardType typeOfEnclosingSwitchExpression;
    private final AnonymousTypeCounters anonymousTypeCounters;

    public static Context create(Runtime runtime,
                                 Summary summary,
                                 Resolver resolver,
                                 TypeContext typeContext) {
        MethodResolutionImpl methodResolution = new MethodResolutionImpl(runtime);
        return new ContextImpl(new Data(runtime, summary, methodResolution),
                null, null, null, resolver,
                typeContext, new VariableContextImpl(), new AnonymousTypeCountersImpl(), null);
    }

    private ContextImpl(Data data, TypeInfo enclosingType,
                        MethodInfo enclosingMethod,
                        FieldInfo enclosingField,
                        Resolver resolver,
                        TypeContext typeContext,
                        VariableContext variableContext,
                        AnonymousTypeCounters anonymousTypeCounters,
                        ForwardType typeOfEnclosingSwitchExpression) {
        this.data = data;
        this.enclosingType = enclosingType;
        this.enclosingMethod = enclosingMethod;
        this.enclosingField = enclosingField;
        this.typeContext = typeContext;
        this.variableContext = variableContext;
        this.typeOfEnclosingSwitchExpression = typeOfEnclosingSwitchExpression;
        this.anonymousTypeCounters = anonymousTypeCounters;
        this.resolver = resolver;
    }

    public Info info() {
        if (enclosingField != null) return enclosingField;
        if (enclosingMethod != null) return enclosingMethod;
        return enclosingType;
    }

    @Override
    public Runtime runtime() {
        return data.runtime;
    }

    @Override
    public MethodResolution methodResolution() {
        return data.methodResolution;
    }

    @Override
    public Resolver resolver() {
        return resolver;
    }


    @Override
    public TypeInfo enclosingType() {
        return enclosingType;
    }


    @Override
    public MethodInfo enclosingMethod() {
        return enclosingMethod;
    }


    @Override
    public FieldInfo enclosingField() {
        return enclosingField;
    }

    @Override
    public TypeContext typeContext() {
        return typeContext;
    }

    @Override
    public VariableContext variableContext() {
        return variableContext;
    }

    @Override
    public Summary summary() {
        return data.summary;
    }


    @Override
    public AnonymousTypeCounters anonymousTypeCounters() {
        return anonymousTypeCounters;
    }

    @Override
    public VariableContext dependentVariableContext() {
        return variableContext.newVariableContext();
    }

    public ContextImpl newCompilationUnit(CompilationUnit compilationUnit) {
        TypeContext typeContext = typeContext().newCompilationUnit(compilationUnit);
        return new ContextImpl(data, null, null, null, resolver,
                typeContext, variableContext.newEmpty(), anonymousTypeCounters.newEmpty(), null);
    }

    @Override
    public Context newVariableContext(String reason, VariableContext variableContext) {
        LOGGER.debug("Creating a new variable context for {}", reason);
        return new ContextImpl(data, enclosingType, enclosingMethod, enclosingField, resolver,
                typeContext, variableContext, anonymousTypeCounters, typeOfEnclosingSwitchExpression);
    }

    @Override
    public Context newVariableContext(String reason) {
        LOGGER.debug("Creating a new variable context for {}", reason);
        VariableContext newVariableContext = variableContext.newVariableContext();
        return new ContextImpl(data, enclosingType, enclosingMethod, enclosingField, resolver,
                typeContext, newVariableContext, anonymousTypeCounters, typeOfEnclosingSwitchExpression);
    }

    @Override
    public Context newVariableContextForMethodBlock(MethodInfo methodInfo, ForwardType forwardType) {
        return new ContextImpl(data, methodInfo.typeInfo(), methodInfo, null, resolver,
                typeContext, dependentVariableContext(), anonymousTypeCounters, forwardType);
    }

    public ContextImpl newAnonymousClassBody(TypeInfo baseType) {
        TypeContext newTypeContext = typeContext.newAnonymousClassBody(baseType);
        VariableContext newVariableContext = variableContext.newVariableContext();
        return new ContextImpl(data, baseType, null, null, resolver.newEmpty(), newTypeContext,
                newVariableContext, anonymousTypeCounters, typeOfEnclosingSwitchExpression);
    }


    public ContextImpl newSubType(TypeInfo subType) {
        return new ContextImpl(data, subType, null, null, resolver,
                typeContext.newTypeContext(), variableContext, anonymousTypeCounters, null);
    }

    @Override
    public Context newLambdaContext(MethodInfo sam) {
        return new ContextImpl(data, sam.typeInfo(), sam, null, resolver, typeContext.newTypeContext(),
                variableContext.newVariableContext(), anonymousTypeCounters, typeOfEnclosingSwitchExpression);
    }

    @Override
    public ForwardType newForwardType(ParameterizedType parameterizedType) {
        return new ForwardTypeImpl(parameterizedType, false, TypeParameterMap.EMPTY);
    }

    @Override
    public ForwardType newForwardType(ParameterizedType parameterizedType,
                                      boolean erasure,
                                      Map<NamedType, ParameterizedType> typeParameterMap) {
        return new ForwardTypeImpl(parameterizedType, erasure, new TypeParameterMap(typeParameterMap));
    }

    @Override
    public Context newTypeBody() {
        VariableContext newVariableContext = variableContext.newEmpty();
        typeContext.staticFieldImports(runtime()).values().forEach(newVariableContext::add);
        return new ContextImpl(data, enclosingType, enclosingMethod, enclosingField, resolver, typeContext,
                newVariableContext, anonymousTypeCounters, typeOfEnclosingSwitchExpression);
    }

    @Override
    public Context newTypeContext() {
        TypeContext newTypeContext = typeContext.newTypeContext();
        return new ContextImpl(data, enclosingType, enclosingMethod, enclosingField, resolver, newTypeContext,
                variableContext, anonymousTypeCounters, typeOfEnclosingSwitchExpression);
    }

    @Override
    public ForwardType erasureForwardType() {
        return new ForwardTypeImpl(null, true, TypeParameterMap.EMPTY);
    }

    public ForwardType emptyForwardType() {
        return new ForwardTypeImpl(null, false, TypeParameterMap.EMPTY);
    }

    @Override
    public ParseHelper parseHelper() {
        return resolver().parseHelper();
    }

    @Override
    public GenericsHelper genericsHelper() {
        return data.methodResolution.genericsHelper();
    }
}
