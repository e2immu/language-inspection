package org.e2immu.language.inspection.impl.parser;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.SourceTypes;
import org.e2immu.util.internal.util.Trie;

import java.util.List;
import java.util.function.BiConsumer;

public class SourceTypesImpl implements SourceTypes {

    private final Trie<TypeInfo> trie = new Trie<>();

    @Override
    public TypeInfo get(String fqn) {
        List<TypeInfo> types = trie.get(fqn.split("\\."));
        return types == null ? null : types.get(0);
    }

    @Override
    public boolean isKnown(TypeInfo typeInfo) {
        return false;
    }

    @Override
    public void visit(String[] split, BiConsumer<String[], List<TypeInfo>> biConsumer) {
        trie.visit(split, biConsumer);
    }

    @Override
    public void add(String[] parts, TypeInfo typeInfo) {
        trie.add(parts, typeInfo);
    }

    @Override
    public void freeze() {
        trie.freeze();
    }
}
