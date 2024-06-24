package org.e2immu.language.inspection.impl.parser;

import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;

import org.e2immu.language.inspection.api.parser.*;
import org.e2immu.language.inspection.api.resource.TypeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ContextImpl implements Context {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContextImpl.class);

    private record Data(Runtime runtime, Summary summary, Resolver resolver) {
    }

    private final Data data;
    private final TypeInfo enclosingType;
    private final MethodInfo enclosingMethod;
    private final FieldInfo enclosingField;
    private final TypeContext typeContext;
    private final VariableContext variableContext;
    private final ForwardType typeOfEnclosingSwitchExpression;
    private final AnonymousTypeCounters anonymousTypeCounters;

    public ContextImpl(Runtime runtime,
                       Summary summary,
                       Resolver resolver,
                       TypeContext typeContext,
                       VariableContext variableContext,
                       AnonymousTypeCounters anonymousTypeCounters,
                       ForwardType typeOfEnclosingSwitchExpression) {
        this(new Data(runtime, summary, resolver), null, null, null,
                typeContext, variableContext, anonymousTypeCounters, typeOfEnclosingSwitchExpression);
    }

    private ContextImpl(Data data, TypeInfo enclosingType,
                        MethodInfo enclosingMethod,
                        FieldInfo enclosingField, TypeContext typeContext,
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
    public Resolver resolver() {
        return data.resolver;
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


    public ForwardType typeOfEnclosingSwitchExpression() {
        return typeOfEnclosingSwitchExpression;
    }

    public ContextImpl newCompilationUnit(Resolver resolver, TypeMap.Builder typeMap, CompilationUnit compilationUnit) {
        TypeContext typeContext = typeContext().newCompilationUnit(typeMap, compilationUnit);
        return new ContextImpl(data, null, null, null,
                typeContext, variableContext.newEmpty(), anonymousTypeCounters.newEmpty(), null);
    }

    @Override
    public Context newVariableContext(String reason, VariableContext variableContext) {
        LOGGER.debug("Creating a new variable context for {}", reason);
        return new ContextImpl(data, enclosingType, enclosingMethod, enclosingField,
                typeContext, variableContext, anonymousTypeCounters, typeOfEnclosingSwitchExpression);
    }

    @Override
    public Context newVariableContext(String reason) {
        LOGGER.debug("Creating a new variable context for {}", reason);
        VariableContext newVariableContext = variableContext.newVariableContext();
        return new ContextImpl(data, enclosingType, enclosingMethod, enclosingField,
                typeContext, newVariableContext, anonymousTypeCounters, typeOfEnclosingSwitchExpression);
    }

    @Override
    public Context newVariableContextForMethodBlock(MethodInfo methodInfo, ForwardType forwardType) {
        return new ContextImpl(data, methodInfo.typeInfo(), methodInfo, null,
                typeContext, dependentVariableContext(), anonymousTypeCounters, forwardType);
    }


    public ContextImpl newTypeContext(String reason) {
        LOGGER.debug("Creating a new type context for {}", reason);
        return new ContextImpl(data, enclosingType, enclosingMethod, enclosingField,
                typeContext.newTypeContext(), variableContext, anonymousTypeCounters, typeOfEnclosingSwitchExpression);
    }


    public ContextImpl newTypeContext(FieldInfo fieldInfo) {
        return new ContextImpl(data, enclosingType, null, fieldInfo, typeContext,
                variableContext, anonymousTypeCounters, typeOfEnclosingSwitchExpression);
    }


    public ContextImpl newAnonymousClassBody(TypeInfo baseType) {
        throw new UnsupportedOperationException();
    }


    public ContextImpl newSubType(TypeInfo subType) {
        return new ContextImpl(data, subType, null, null,
                typeContext.newTypeContext(), variableContext, anonymousTypeCounters, null);
    }


    public ContextImpl newSwitchExpressionContext(TypeInfo subType,
                                                  VariableContext variableContext,
                                                  ForwardType typeOfEnclosingSwitchExpression) {
        return new ContextImpl(data, subType, null, null, typeContext,
                variableContext, anonymousTypeCounters, typeOfEnclosingSwitchExpression);
    }


    public ContextImpl newLambdaContext(TypeInfo subType, VariableContext variableContext) {
        throw new UnsupportedOperationException();
    }


    public ForwardType newForwardType(ParameterizedType parameterizedType) {
        return new ForwardTypeImpl(parameterizedType, false, TypeParameterMap.EMPTY);
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
}
