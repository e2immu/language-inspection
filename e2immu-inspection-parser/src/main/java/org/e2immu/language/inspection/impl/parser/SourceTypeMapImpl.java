package org.e2immu.language.inspection.impl.parser;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.SourceTypeMap;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SourceTypeMapImpl implements SourceTypeMap {
    private final TreeMap<String, TypeInfo> map = new TreeMap<>();

    public void putAll(Map<String, TypeInfo> map) {
        this.map.putAll(map);
    }

    @Override
    public TypeInfo get(String fullyQualifiedName) {
        return map.get(fullyQualifiedName);
    }

    @Override
    public List<TypeInfo> inPackage(String packageName) {
        List<TypeInfo> result = new LinkedList<>();
        Map.Entry<String, TypeInfo> lower = map.floorEntry(packageName);
        while (accept(lower, packageName)) {
            result.add(lower.getValue());
            lower = map.floorEntry(lower.getKey());
        }
        return result;
    }

    private static boolean accept(Map.Entry<String, TypeInfo> lower, String packageName) {
        if (lower == null) return false;
        String fqn = lower.getKey();
        if (!fqn.startsWith(packageName)) return false;
        int lastDot = fqn.lastIndexOf('.');
        return packageName.equals(fqn.substring(0, lastDot));
    }
}
