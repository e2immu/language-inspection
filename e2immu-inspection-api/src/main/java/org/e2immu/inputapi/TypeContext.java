package org.e2immu.inputapi;

import org.e2immu.cstapi.info.TypeInfo;
import org.e2immu.cstapi.type.TypeParameter;

import java.util.Spliterator;

public interface TypeContext {
    default TypeInfo getFullyQualified(Class<?> clazz) {
        return getFullyQualified(clazz.getCanonicalName(), true);
    }

    TypeInfo getFullyQualified(String fqn, boolean complain);

    void addToContext(TypeParameter typeParameter);

    TypeContext newTypeContext();

    TypeMap typeMap();
}
