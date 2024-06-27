package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.cst.api.expression.Lambda;
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

            class MethodCall_0 {

                record Get(String s) {
                    String get() {
                        return s;
                    }
                }

                void accept(List<Get> list) {
                    list.forEach(get -> System.out.println(get.get()));
                }

                void test() {
                    accept(List.of(new Get("hello")));
                }

                void test2() {
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
        if (accept.methodBody().statements().get(0) instanceof ExpressionAsStatement eas
            && eas.expression() instanceof MethodCall mc) {
            assertEquals("java.lang.Iterable.forEach(java.util.function.Consumer<? super T>)",
                    mc.methodInfo().fullyQualifiedName());
            if (mc.parameterExpressions().get(0) instanceof Lambda lambda) {
                assertEquals("Type java.util.function.Consumer<org.e2immu.analyser.resolver.testexample.MethodCall_0.Get>",
                        lambda.concreteFunctionalType().toString());
            } else fail();
        } else fail();
    }


    @Language("java")
    private static final String INPUT3 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.List;

            public class MethodCall_1 {

                interface Get {
                    String get();
                }

                record GetOnly(String s) implements Get {

                    @Override
                    public String get() {
                        return s;
                    }
                }

                public void accept(List<Get> list) {
                    list.forEach(get -> System.out.println(get.get()));
                }

                public void test() {
                    // here, List.of(...) becomes a List<Get> because of the context of 'accept(...)'
                    accept(List.of(new GetOnly("hello")));
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = javaInspector.parse(INPUT3);
        MethodInfo test = typeInfo.findUniqueMethod("test", 0);
        if (test.methodBody().statements().get(0) instanceof ExpressionAsStatement eas
            && eas.expression() instanceof MethodCall mc1
            && mc1.parameterExpressions().get(0) instanceof MethodCall mc2) {
            assertEquals("Type java.util.List<org.e2immu.analyser.resolver.testexample.MethodCall_1.GetOnly>",
                    mc2.concreteReturnType().toString());
        } else fail();
    }


    @Language("java")
    private static final String INPUT4 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.List;

            public class MethodCall_2 {

                interface Get {
                    String get();
                }

                interface Set extends Get {
                    void set(String s);
                }

                static class Both implements Set {
                    private String s;

                    public Both(String s) {
                        this.s = s;
                    }

                    @Override
                    public String get() {
                        return s;
                    }

                    @Override
                    public void set(String s) {
                        this.s = s;
                    }
                }

                public void accept(List<Get> list) {
                    list.forEach(get -> System.out.println(get.get()));
                }

                public void test() {
                    // here, List.of(...) becomes a List<Get> because of the context of 'accept(...)'
                    accept(List.of(new Both("hello")));
                }
            }
            """;

    @Test
    public void test4() {
        javaInspector.parse(INPUT4);
    }

    @Language("java")
    private static final String INPUT5 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.List;

            class MethodCall_3 {

                interface Get {
                    String get();
                }

                public void accept(List<Get> list) {
                    list.forEach(get -> System.out.println(get.get()));
                }

                public void test() {
                    accept(List.of(() -> "hello"));
                }
            }
            """;

    @Test
    public void test5() {
        javaInspector.parse(INPUT5);
    }

    @Language("java")
    private static final String INPUT6 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.ArrayList;
            import java.util.List;

            public class MethodCall_4 {
                private static List<String> copy(List<String> list) {
                    return new ArrayList<>(list);
                }

                public static int length(List<String> list) {
                    return copy(list).size();
                }
            }
            """;

    @Test
    public void test6() {
        TypeInfo typeInfo = javaInspector.parse(INPUT6);

    }

    @Language("java")
    private static final String INPUT7 = """
            """;

    @Test
    public void test7() {
        TypeInfo typeInfo = javaInspector.parse(INPUT4);

    }


}
