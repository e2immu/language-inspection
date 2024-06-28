package org.e2immu.language.inspection.integration;

import org.e2immu.analyzer.shallow.aapi.WriteAnalysis;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.util.internal.util.Trie;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestWriteAnalysis {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestWriteAnalysis.class);

    private final Runtime runtime = new RuntimeImpl();

    @Test
    public void test() throws IOException {
        CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName("org.e2immu").build();
        TypeInfo typeInfo = runtime.newTypeInfo(cu, "C");

        typeInfo.analysis().set(PropertyImpl.IMMUTABLE_TYPE, new ValueImpl.ImmutableImpl(3));
        typeInfo.analysis().set(PropertyImpl.SHALLOW_ANALYZER, ValueImpl.BoolImpl.TRUE);
        typeInfo.analysis().set(PropertyImpl.COMMUTABLE_METHODS,
                new ValueImpl.CommutableDataImpl("p1", "p2,p3", "p4"));

        MethodInfo methodInfo = runtime.newMethod(typeInfo, "m1", runtime.methodTypeMethod());
        methodInfo.analysis().set(PropertyImpl.SHALLOW_ANALYZER, ValueImpl.BoolImpl.FALSE);
        typeInfo.builder().addMethod(methodInfo);

        WriteAnalysis wa = new WriteAnalysis();
        Trie<TypeInfo> trie = new Trie<>();
        trie.add(new String[]{"org", "e2immu"}, typeInfo);
        File dir = new File("build");
        File targetFile = new File(dir, "OrgE2Immu.json");
        if (targetFile.delete()) LOGGER.debug("Deleted {}", targetFile);
        wa.write(dir.getAbsolutePath(), trie);
        String s = Files.readString(targetFile.toPath());
        assertEquals("""
                [{"fqn": "Torg.e2immu.C", "data":{"immutableType":3,"commutableMethods":["p1","p2,p3","p4"],"shallowAnalyzer":true}},
                {"fqn": "M?.?.m1", "data":{"shallowAnalyzer":false}}]\
                """, s);
    }
}
