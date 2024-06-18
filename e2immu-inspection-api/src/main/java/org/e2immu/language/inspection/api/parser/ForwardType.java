package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.type.ParameterizedType;

public interface ForwardType {
    boolean mustBeArray();

    ForwardType withMustBeArray();

    ParameterizedType type();
}
