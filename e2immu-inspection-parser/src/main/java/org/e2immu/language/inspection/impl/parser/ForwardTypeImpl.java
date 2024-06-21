package org.e2immu.language.inspection.impl.parser;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.api.parser.ForwardType;
import org.e2immu.language.inspection.api.parser.GenericsHelper;
import org.e2immu.language.inspection.api.parser.MethodTypeParameterMap;
import org.e2immu.language.inspection.api.parser.TypeParameterMap;

public record ForwardTypeImpl(ParameterizedType type, boolean mustBeArray, TypeParameterMap extra) implements ForwardType {

    public ForwardTypeImpl() {
        this(null, false, TypeParameterMap.EMPTY);
    }

    public ForwardTypeImpl(ParameterizedType type) {
        this(type, false, TypeParameterMap.EMPTY);
    }

    public ForwardTypeImpl(ParameterizedType type, boolean erasure) {
        this(type, erasure, TypeParameterMap.EMPTY);
    }

    @Override
    public ForwardTypeImpl withMustBeArray() {
        return new ForwardTypeImpl(type, true, extra);
    }

    // we'd rather have java.lang.Boolean, because as soon as type parameters are involved, primitives
    // are boxed
    public static ForwardTypeImpl expectBoolean(Runtime runtime) {
        return new ForwardTypeImpl(runtime.boxedBooleanTypeInfo().asSimpleParameterizedType(), false);
    }

    @Override
    public MethodTypeParameterMap computeSAM(Runtime runtime, GenericsHelper genericsHelper, TypeInfo primaryType) {
        if (type == null || type.isVoid()) return null;
        MethodTypeParameterMap sam = genericsHelper.findSingleAbstractMethodOfInterface(type, false);
        if (sam != null) {
            return sam.expand(runtime, primaryType, type.initialTypeParameterMap(runtime));
        }
        return null;
    }

    @Override
    public boolean isVoid(Runtime runtime, GenericsHelper genericsHelper) {
        if (type == null || type.typeInfo() == null) return false;
        if (type.isVoid()) return true;
        MethodInfo sam = type.typeInfo().singleAbstractMethod();
        if (sam == null) return false;
        MethodTypeParameterMap samMap = genericsHelper.findSingleAbstractMethodOfInterface(type, true);
        assert samMap != null;
        return samMap.getConcreteReturnType(runtime).isVoid();
    }

    @Override
    public String toString() {
        return "[FWD: " + (type == null ? "null" : type.detailedString()) + ", array? " + mustBeArray + "]";
    }
}
