package org.e2immu.language.inspection.integration.java.stub;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;

import static org.junit.jupiter.api.Assertions.*;

public class TestStub1 extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.test;
            
            public class X {
                private Y y;
                public X(Y y) {
                    this.y = y;
                }
            }
            """;


    @Test
    public void test() {
        assertThrows(Summary.FailFastException.class, () ->
                javaInspector.parse(INPUT1, new JavaInspectorImpl.ParseOptionsBuilder()
                        .setFailFast(true).setAllowCreationOfStubTypes(false).build()));
    }

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1, new JavaInspectorImpl.ParseOptionsBuilder()
                .setFailFast(true).setAllowCreationOfStubTypes(true).build());
        FieldInfo y = typeInfo.getFieldByName("y", true);
        TypeInfo Y = y.type().bestTypeInfo();
        assertEquals("Y", Y.fullyQualifiedName());
        assertTrue(Y.typeNature().isStub());
        MethodInfo constructor = typeInfo.findConstructor(1);
        ParameterInfo constructor0 = constructor.parameters().get(0);
        assertEquals(Y, constructor0.parameterizedType().typeInfo());
        assertSame(Y, constructor0.parameterizedType().typeInfo());
    }
}
