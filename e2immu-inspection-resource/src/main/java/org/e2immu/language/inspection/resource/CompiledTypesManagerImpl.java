package org.e2immu.language.inspection.resource;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.resource.ByteCodeInspector;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.Resources;
import org.e2immu.language.inspection.api.resource.SourceFile;
import org.e2immu.support.SetOnce;
import org.e2immu.util.internal.util.Trie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CompiledTypesManagerImpl implements CompiledTypesManager {
    private final Logger LOGGER = LoggerFactory.getLogger(CompiledTypesManagerImpl.class);

    private final Resources classPath;
    private final SetOnce<ByteCodeInspector> byteCodeInspector = new SetOnce<>();
    private final Map<String, TypeInfo> typeMap = new HashMap<>();
    //private final ReentrantReadWriteLock typeMapLock = new ReentrantReadWriteLock(); TODO add locking
    private final Trie<TypeInfo> typeTrie = new Trie<>();
    private final Set<TypeInfo> inspectionQueue = new HashSet<>();

    public CompiledTypesManagerImpl(Resources classPath) {
        this.classPath = classPath;
    }

    @Override
    public void loadByteCodeQueue() {
        LOGGER.debug("Have queue of size {}", inspectionQueue.size());
    }

    @Override
    public void addToQueue(TypeInfo typeInfo) {
        synchronized (inspectionQueue) {
            inspectionQueue.add(typeInfo);
        }
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
        TypeInfo previous = typeMap.put(typeInfo.fullyQualifiedName(), typeInfo);
        assert previous == null;
        String[] parts = typeInfo.fullyQualifiedName().split("\\.");
        typeTrie.add(parts, typeInfo);
    }

    @Override
    public SourceFile fqnToPath(String fqn, String suffix) {
        return classPath.fqnToPath(fqn, suffix);
    }

    @Override
    public TypeInfo get(String fullyQualifiedName) {
        return typeMap.get(fullyQualifiedName);
    }

    @Override
    public TypeInfo getOrLoad(String fullyQualifiedName) {
        TypeInfo typeInfo = typeMap.get(fullyQualifiedName);
        if (typeInfo != null) return typeInfo;
        SourceFile path = fqnToPath(fullyQualifiedName, ".class");
        if (path == null) return null;
        return byteCodeInspector.get().load(path);
    }

    @Override
    public void ensureInspection(TypeInfo typeInfo) {
        if (!typeInfo.hasBeenInspected()) {
            SourceFile sourceFile = fqnToPath(typeInfo.fullyQualifiedName(), ".class");
            if (sourceFile == null) throw new UnsupportedOperationException("Cannot find .class file for " + typeInfo);
            byteCodeInspector.get().load(typeInfo);
        }
    }

    @Override
    public TypeInfo load(SourceFile path) {
        // only to be used when the type does not yet exist!
        return byteCodeInspector.get().load(path);
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

    @Override
    public void preload(String thePackage) {
        LOGGER.info("Start pre-loading {}", thePackage);
        AtomicInteger inspected = new AtomicInteger();
        classPath.expandLeaves(thePackage, ".class", (expansion, list) -> {
            // we'll loop over the primary types only
            if (!expansion[expansion.length - 1].contains("$")) {
                String fqn = fqnOfClassFile(thePackage, expansion);
                assert acceptFQN(fqn);
                TypeInfo typeInfo = typeMap.get(fqn);
                if (typeInfo == null) {
                    SourceFile path = fqnToPath(fqn, ".class");
                    if (path != null) {
                        byteCodeInspector.get().load(path);
                        inspected.incrementAndGet();
                    }
                }
            }
        });
        LOGGER.info("... inspected {} paths", inspected);
    }

    private String fqnOfClassFile(String prefix, String[] suffixes) {
        String combined = prefix + "." + String.join(".", suffixes).replaceAll("\\$", ".");
        if (combined.endsWith(".class")) {
            return combined.substring(0, combined.length() - 6);
        }
        throw new UnsupportedOperationException("Expected .class or .java file, but got " + combined);
    }

    @Override
    public List<TypeInfo> typesLoaded() {
        return typeMap.values().stream().sorted(Comparator.comparing(TypeInfo::fullyQualifiedName)).toList();
    }
}
