package org.e2immu.inputapi;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.TypeParameter;

public interface TypeContext {
    default TypeInfo getFullyQualified(Class<?> clazz) {
        return getFullyQualified(clazz.getCanonicalName(), true);
    }

    TypeInfo getFullyQualified(String fqn, boolean complain);

    void addToContext(TypeParameter typeParameter);

    TypeContext newTypeContext();

    TypeMap typeMap();
}
