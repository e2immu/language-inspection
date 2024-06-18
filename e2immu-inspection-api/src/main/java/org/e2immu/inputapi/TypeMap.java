package org.e2immu.inputapi;

import org.e2immu.cstapi.info.TypeInfo;

public interface TypeMap {
    void add(TypeInfo typeInfo, InspectionState inspectionState);

    void addToByteCodeQueue(String fqn);

    TypeInfo addToTrie(TypeInfo subType);

    default TypeInfo get(Class<?> clazz) {
        return get(clazz.getCanonicalName(), true);
    }

    TypeInfo get(String name, boolean complain);

    String pathToFqn(String interfaceName);

    InspectionAndState typeInspectionSituation(String fqn);

    record InspectionAndState(TypeInfo typeInfo, InspectionState state) {
    }

}
