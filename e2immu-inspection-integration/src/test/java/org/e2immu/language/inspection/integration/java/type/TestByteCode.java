package org.e2immu.language.inspection.integration.java.type;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.TypeParameter;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.AbstractMockMvcBuilder;

import java.io.FileOutputStream;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestByteCode extends CommonTest {
    @Test
    public void test() {
        TypeInfo typeInfo = javaInspector.compiledTypesManager().getOrLoad(AbstractMockMvcBuilder.class);
        assertEquals("""
                B=TP#0 in AbstractMockMvcBuilder [Type org.springframework.test.web.servlet.setup.AbstractMockMvcBuilder<\
                B extends org.springframework.test.web.servlet.setup.AbstractMockMvcBuilder<B>>]\
                """, typeInfo.typeParameters().stream()
                .map(TypeParameter::toStringWithTypeBounds).collect(Collectors.joining(", ")));
        MethodInfo apply = typeInfo.findUniqueMethod("apply", 1);
        assertEquals("""
                T=TP#0 in AbstractMockMvcBuilder.apply [Type param B extends \
                org.springframework.test.web.servlet.setup.AbstractMockMvcBuilder<B>]\
                """, apply.typeParameters().stream()
                .map(TypeParameter::toStringWithTypeBounds).collect(Collectors.joining(", ")));
    }

    @Test
    public void testThrows() {
        TypeInfo typeInfo = javaInspector.compiledTypesManager().getOrLoad(FileOutputStream.class);
        MethodInfo close = typeInfo.findUniqueMethod("close", 0);
        assertEquals("java.io.FileOutputStream.close()", close.fullyQualifiedName());
        assertEquals("Type java.io.IOException", close.exceptionTypes().getFirst().toString());
    }

    @Test
    public void testThrows2() {
        TypeInfo typeInfo = javaInspector.compiledTypesManager().getOrLoad("java.lang.ScopedValue");
        TypeInfo carrier = typeInfo.findSubType("Carrier");
        MethodInfo close = carrier.findUniqueMethod("call", 1);
        assertEquals("java.lang.ScopedValue.Carrier.call(CallableOp<? extends R,X extends Throwable>)",
                close.fullyQualifiedName());
        assertEquals("Type param X extends Throwable", close.exceptionTypes().getFirst().toString());
    }
}
