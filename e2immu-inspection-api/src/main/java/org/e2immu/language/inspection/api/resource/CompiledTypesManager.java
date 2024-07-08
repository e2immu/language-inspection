package org.e2immu.language.inspection.api.resource;

import org.e2immu.language.cst.api.info.TypeInfo;

import java.util.List;

/**
 * manages all types that come in byte-code form.
 * also deals with the bootstrapping of types (Object, String, etc.)
 * <p>
 * lots of defaults because we make stubs.
 */
public interface CompiledTypesManager {

    default void addToQueue(TypeInfo remote) {
        throw new UnsupportedOperationException();
    }

    default TypeInfo addToTrie(TypeInfo typeInfo) {
        return typeInfo;
    }

    default ByteCodeInspector byteCodeInspector() {
        throw new UnsupportedOperationException();
    }

    default Resources classPath() {
        throw new UnsupportedOperationException();
    }

    default void add(TypeInfo typeInfo) {
        // do nothing
    }

    default SourceFile fqnToPath(String fqn, String suffix) {
        throw new UnsupportedOperationException("Don't know how to load "+fqn);
    }

    default TypeInfo get(Class<?> clazz) {
        return get(clazz.getCanonicalName());
    }

    TypeInfo get(String fullyQualifiedName);

    default TypeInfo getOrLoad(String fullyQualifiedName) {
        return get(fullyQualifiedName);
    }

    default TypeInfo getOrLoad(Class<?> clazz) {
        return getOrLoad(clazz.getCanonicalName());
    }

    default void ensureInspection(TypeInfo typeInfo) {
        // do nothing
    }

    default TypeInfo load(SourceFile path) {
        throw new UnsupportedOperationException();
    }

    default void loadByteCodeQueue() {
        throw new UnsupportedOperationException();
    }

    default void preload(String thePackage) {
        throw new UnsupportedOperationException();
    }

    default void setLazyInspection(TypeInfo typeInfo) {
        // do nothing
    }

    default boolean acceptFQN(String fqn) {
        return !fqn.startsWith("jdk.internal.");
    }
}
