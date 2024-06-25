package org.e2immu.language.inspection.resource;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.resource.ByteCodeInspector;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.Resources;
import org.e2immu.language.inspection.api.resource.SourceFile;

public class CompiledTypesManagerImpl implements CompiledTypesManager {
    public CompiledTypesManagerImpl(Runtime runtime, Resources classPath) {
    }

    @Override
    public ByteCodeInspector byteCodeInspector() {
        return null;
    }

    @Override
    public Resources classPath() {
        return null;
    }

    @Override
    public void add(TypeInfo typeInfo) {

    }

    @Override
    public SourceFile fqnToPath(String fqn, String suffix) {
        return null;
    }

    @Override
    public TypeInfo get(String fullyQualifiedName) {
        return null;
    }

    @Override
    public TypeInfo getOrCreate(String fullyQualifiedName, boolean complain) {
        return null;
    }

    @Override
    public void ensureInspection(TypeInfo typeInfo) {

    }

    @Override
    public TypeInfo load(SourceFile path) {
        return null;
    }

    public void setByteCodeInspector(ByteCodeInspector byteCodeInspector) {
    }

    @Override
    public void setLazyInspection(TypeInfo typeInfo) {

    }
}
