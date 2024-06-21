package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;

public interface ForwardType {
    MethodTypeParameterMap computeSAM(Runtime runtime, GenericsHelper genericsHelper, TypeInfo primaryType);

    boolean isVoid(Runtime runtime, GenericsHelper genericsHelper);

    ParameterizedType type();

    ForwardType withMustBeArray();
}