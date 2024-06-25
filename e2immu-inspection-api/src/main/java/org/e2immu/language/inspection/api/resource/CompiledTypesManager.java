package org.e2immu.language.inspection.api.resource;

import org.e2immu.language.cst.api.info.TypeInfo;

import java.net.URI;

/**
 * manages all types that come in byte-code form.
 * also deals with the bootstrapping of types (Object, String, etc.)
 */
public interface CompiledTypesManager {

    ByteCodeInspector byteCodeInspector();

    Resources classPath();

    void add(TypeInfo typeInfo);

    SourceFile fqnToPath(String fqn, String suffix);

    TypeInfo get(String fullyQualifiedName);

    TypeInfo getOrCreate(String fullyQualifiedName, boolean complain);

    void ensureInspection(TypeInfo typeInfo);

    TypeInfo load(SourceFile path);

    void setLazyInspection(TypeInfo typeInfo);

    default boolean acceptFQN(String fqn) {
        return !fqn.startsWith("jdk.internal.");
    }
}
