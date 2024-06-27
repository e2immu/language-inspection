package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class TestMethodCall1_Basics extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.test;

            import java.util.ArrayList;import java.util.List;

            public class MethodCall_0 {

                private List<String> list;

                public MethodCall_0() {
                   list = new ArrayList<>();
                }
                public void add(String s) {
                   list.add(s);
                }
                public String get(String s) {
                   return list.get(0);
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        FieldInfo fieldInfo = typeInfo.getFieldByName("list", true);
        assertEquals("java.util.List", fieldInfo.type().typeInfo().fullyQualifiedName());
        MethodInfo add = typeInfo.findUniqueMethod("add", 1);
        if (add.methodBody().statements().get(0) instanceof ExpressionAsStatement eas
            && eas.expression() instanceof MethodCall mc) {
            assertEquals("java.util.List.add(E)", mc.methodInfo().fullyQualifiedName());
        } else fail();

        MethodInfo get = typeInfo.findUniqueMethod("get", 1);
        if (get.methodBody().statements().get(0) instanceof ReturnStatement rs
            && rs.expression() instanceof MethodCall mc) {
            assertEquals("java.util.List.get(int)", mc.methodInfo().fullyQualifiedName());
        } else fail();
    }

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.List;

            public class MethodCall_0 {

                record Get(String s) {
                    String get() {
                        return s;
                    }
                }

                public void accept(List<Get> list) {
                    list.forEach(get -> System.out.println(get.get()));
                }

                public void test() {
                    accept(List.of(new Get("hello")));
                }

                public void test2() {
                    accept(null);
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo consumer = javaInspector.compiledTypesManager().get(Consumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.hasBeenInspected());

        TypeInfo typeInfo = javaInspector.parse(INPUT2);
        TypeInfo subType = typeInfo.findSubType("Get");
        MethodInfo accept = typeInfo.findUniqueMethod("accept", 1);
        ParameterInfo list = accept.parameters().get(0);
        assertSame(subType, list.parameterizedType().parameters().get(0).typeInfo());
    }
}
