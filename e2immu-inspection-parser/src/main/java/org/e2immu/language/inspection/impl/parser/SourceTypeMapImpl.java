package org.e2immu.language.inspection.impl.parser;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.SourceTypeMap;

import java.util.HashMap;
import java.util.Map;

public class SourceTypeMapImpl implements SourceTypeMap {
    private final Map<String, TypeInfo> map = new HashMap<>();

    public void putAll(Map<String, TypeInfo> map) {
        this.map.putAll(map);
    }

    @Override
    public TypeInfo get(String fullyQualifiedName) {
        return map.get(fullyQualifiedName);
    }
}
