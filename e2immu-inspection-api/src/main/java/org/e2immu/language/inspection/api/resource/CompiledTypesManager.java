package org.e2immu.language.inspection.api.resource;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * manages all types that come in byte-code form.
 * also deals with the bootstrapping of types (Object, String, etc.)
 * <p>
 * lots of defaults because we make stubs.
 */
public interface CompiledTypesManager {

    default Resources classPath() {
        throw new UnsupportedOperationException();
    }

    default void add(TypeInfo typeInfo) {
        // do nothing
    }

    default SourceFile fqnToPath(String fqn, String suffix) {
        throw new UnsupportedOperationException("Don't know how to load " + fqn);
    }

    default TypeInfo get(Class<?> clazz) {
        return get(clazz.getCanonicalName(), null);
    }

    TypeInfo get(String fullyQualifiedName, SourceSet sourceSetOfRequest);

    default TypeInfo getOrLoad(String fullyQualifiedName, SourceSet sourceSetOfRequest) {
        return get(fullyQualifiedName, sourceSetOfRequest);
    }

    default TypeInfo getOrLoad(Class<?> clazz) {
        return getOrLoad(clazz.getCanonicalName(), null);
    }

    default void ensureInspection(TypeInfo typeInfo) {
        // do nothing
    }

    default TypeInfo load(SourceFile path) {
        throw new UnsupportedOperationException();
    }

    default boolean packageContainsTypes(String packageName) {
        throw new UnsupportedOperationException();
    }

    default void preload(String thePackage) {
        throw new UnsupportedOperationException();
    }

    default Collection<TypeInfo> primaryTypesInPackageEnsureLoaded(String packageName, Set<String> fqnToAvoid) {
        throw new UnsupportedOperationException();
    }

    default boolean acceptFQN(String fqn) {
        return !fqn.startsWith("jdk.internal.");
    }

    default List<TypeInfo> typesLoaded() {
        throw new UnsupportedOperationException();
    }
}
