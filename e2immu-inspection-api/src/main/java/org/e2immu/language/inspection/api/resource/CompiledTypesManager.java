package org.e2immu.language.inspection.api.resource;

import org.e2immu.language.cst.api.info.TypeInfo;

import java.net.URI;

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
        throw new UnsupportedOperationException();
    }

    default TypeInfo get(Class<?> clazz) {
        return get(clazz.getCanonicalName());
    }

    TypeInfo get(String fullyQualifiedName);

    default TypeInfo getOrCreate(String fullyQualifiedName, boolean complain) {
        return get(fullyQualifiedName);
    }

    default void ensureInspection(TypeInfo typeInfo) {
        // do nothing
    }

    default TypeInfo load(SourceFile path) {
        throw new UnsupportedOperationException();
    }

    default void setLazyInspection(TypeInfo typeInfo) {
        // do nothing
    }

    default boolean acceptFQN(String fqn) {
        return !fqn.startsWith("jdk.internal.");
    }
}
