package org.e2immu.language.inspection.resource;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.resource.ByteCodeInspector;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.Resources;
import org.e2immu.language.inspection.api.resource.SourceFile;
import org.e2immu.support.SetOnce;
import org.e2immu.util.internal.util.Trie;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CompiledTypesManagerImpl implements CompiledTypesManager {
    private final Resources classPath;
    private final SetOnce<ByteCodeInspector> byteCodeInspector = new SetOnce<>();
    private final Map<String, TypeInfo> typeMap = new HashMap<>();
    private final Trie<TypeInfo> typeTrie = new Trie<>();
    private final List<TypeInfo> byteInspectionQueue = new LinkedList<>();

    public CompiledTypesManagerImpl(Resources classPath) {
        this.classPath = classPath;
    }

    @Override
    public void addToQueue(TypeInfo typeInfo) {
        byteInspectionQueue.add(typeInfo);
    }

    @Override
    public ByteCodeInspector byteCodeInspector() {
        return byteCodeInspector.get();
    }

    @Override
    public Resources classPath() {
        return classPath;
    }

    @Override
    public void add(TypeInfo typeInfo) {
        typeMap.put(typeInfo.fullyQualifiedName(), typeInfo);
        String[] parts = typeInfo.fullyQualifiedName().split("\\.");
        typeTrie.add(parts, typeInfo);
    }

    @Override
    public SourceFile fqnToPath(String fqn, String suffix) {
        return null;
    }

    @Override
    public TypeInfo get(String fullyQualifiedName) {
        return typeMap.get(fullyQualifiedName);
    }

    @Override
    public TypeInfo getOrCreate(String fullyQualifiedName, boolean complain) {
        TypeInfo typeInfo = typeMap.get(fullyQualifiedName);
        if (typeInfo != null) return typeInfo;
        if (complain) {
            // first, try to load
            TypeInfo loaded = load(fqnToPath(fullyQualifiedName, ".class"));
            if (loaded != null) {
                return loaded;
            }
            throw new UnsupportedOperationException("Cannot find " + fullyQualifiedName);
        }
        return null;
    }

    @Override
    public void ensureInspection(TypeInfo typeInfo) {
        byteCodeInspector.get().load(fqnToPath(typeInfo.fullyQualifiedName(), ".class"));
    }

    @Override
    public TypeInfo load(SourceFile path) {
        List<TypeInfo> types = byteCodeInspector.get().load(path);
        if (types.isEmpty()) return null;
        return types.get(0);
    }

    public void setByteCodeInspector(ByteCodeInspector byteCodeInspector) {
        this.byteCodeInspector.set(byteCodeInspector);
    }

    @Override
    public void setLazyInspection(TypeInfo typeInfo) {
        if (!typeInfo.haveOnDemandInspection()) {
            typeInfo.setOnDemandInspection(ti -> byteCodeInspector.get().load(fqnToPath(typeInfo.fullyQualifiedName(), ".class")));
        }
    }
}
