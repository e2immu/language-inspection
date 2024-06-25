package org.e2immu.language.inspection.resource;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.InspectionState;
import org.e2immu.language.inspection.api.resource.TypeMap;

import java.util.List;

public class TypeMapImpl  implements TypeMap  {

    @Override
    public void add(TypeInfo typeInfo, InspectionState inspectionState) {

    }

    @Override
    public void addToByteCodeQueue(String fqn) {

    }

    @Override
    public TypeInfo addToTrie(TypeInfo subType) {
        return null;
    }

    @Override
    public TypeInfo get(String fullyQualifiedName) {
        return null;
    }

    @Override
    public TypeInfo get(String name, boolean complain) {
        return null;
    }

    @Override
    public boolean isPackagePrefix(List<String> packagePrefix) {
        return false;
    }

    @Override
    public String pathToFqn(String interfaceName) {
        return "";
    }

    @Override
    public InspectionAndState typeInspectionSituation(String fqn) {
        return null;
    }

    public static class Builder {

    }
}
