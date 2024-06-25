package org.e2immu.language.inspection.api.resource;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.InspectionState;

import java.util.List;

/*
Connects to the Resources maps.
 */
public interface TypeMap {

    void add(TypeInfo typeInfo, InspectionState inspectionState);

    void addToByteCodeQueue(String fqn);

    TypeInfo addToTrie(TypeInfo subType);

    TypeInfo get(String fullyQualifiedName);

    default TypeInfo get(Class<?> clazz) {
        return get(clazz.getCanonicalName(), true);
    }

    TypeInfo get(String name, boolean complain);

    boolean isPackagePrefix(List<String> components);

    String pathToFqn(String interfaceName);

    InspectionAndState typeInspectionSituation(String fqn);

    interface Builder extends TypeMap {
        void ensureInspection(TypeInfo typeInfo);

        // generic, could be from source, could be from byte code; used in direct type access in source code
        TypeInfo getOrCreate(String fqn, boolean complain);
    }


    record InspectionAndState(TypeInfo typeInfo, InspectionState state) {
    }

}
