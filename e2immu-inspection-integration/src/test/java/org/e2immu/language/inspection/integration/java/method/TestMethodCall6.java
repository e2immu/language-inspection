package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

public class TestMethodCall6 extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.lang.reflect.Array;
            import java.util.Arrays;
            import java.util.function.Function;
            import java.util.function.IntFunction;
            import java.util.function.Predicate;

            public class MethodCall_61 {

                interface I extends Cloneable {
                    long getId();
                }

                static <T extends I> T byId(T[] data, long id) {
                    return null;
                }

                interface D extends java.io.Serializable, I {
                }

                void r(D d) {
                }

                void method(D[] ds) {
                    r(byId(ds, 3L));
                }

                private static <T extends I> IntFunction<T[]> getArrayFactory(T[] array) {
                    return (length) -> {
                        Class componentType = array.getClass().getComponentType();
                        return (T[]) Array.newInstance(componentType, length);
                    };
                }

                static <T> Predicate<T> predicate(Function<? super T, ?> keyExtractor) {
                    return t -> true;
                }

                static <T extends I> T[] method2(T[] array) {
                    return Arrays.stream(array).toArray(getArrayFactory(array));
                }

                static <T extends I> T[] method3(T[] array) {
                   return Arrays.stream(array).filter(predicate(I::getId)).toArray(getArrayFactory(array));
                }
            }
            """;

    @Test
    public void test1() {
        javaInspector.parse(INPUT1);
    }

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.ArrayList;
            import java.util.List;

            public class MethodCall_62 {

                interface Expression {
                }

                static Expression or(Expression[] array) {
                    return array[0];
                }

                List<Expression> method(Expression[] array) {
                    List<Expression> ands = new ArrayList<>();
                    ands.add(or(array));
                    return ands;
                }
            }
            """;

    @Test
    public void test2() {
        javaInspector.parse(INPUT2);
    }

    @Language("java")
    private static final String INPUT5 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.List;

            public class MethodCall_65 {

                interface I {

                }

                static class S {
                    List<I> getList() {
                        return null;
                    }
                }

                static class B {
                    D d;

                    public void setD(D value) {
                        this.d = value;
                    }

                    static class D {

                    }
                }

                private B.D create(List<I> list) {
                    return null;
                }

                B method(S s) {
                    B b = new B();
                    b.setD(create(s.getList()));
                    return b;
                }
            }
            """;

    @Test
    public void test5() {
        javaInspector.parse(INPUT5);
    }


    @Language("java")
    private static final String INPUT7 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.ArrayList;
            import java.util.List;

            public class MethodCall_67 {
                List<String> onDemandHistory = new ArrayList<>();

                private void error(String msg, Object object) {
                    System.out.println(msg + ": " + object);
                }

                void method() {
                    error("On-demand history:\\n{}", String.join("\\n", onDemandHistory));
                }
            }
            """;

    @Test
    public void test7() {
        javaInspector.parse(INPUT7);
    }


    @Language("java")
    private static final String INPUT3 = """
            package org.e2immu.analyser.resolver.testexample;

            /*
            simply take the first one, if 'null' is involved
             */
            public class MethodCall_66 {
                static class MyException extends RuntimeException {

                }
                interface Logger {
                    void logError(String msg, MyException myException);

                    void logError(String msg, Throwable throwable);
                }

                void method1(Logger logger) {
                    logger.logError("hello", null);
                }
               \s
                void method2(Logger logger) {
                    logger.logError("hello", (Throwable) null);
                }
            }
            """;

    @Test
    public void test3() {
        javaInspector.parse(INPUT3);
    }

    @Language("java")
    private static final String INPUT8 = """
            package org.e2immu.analyser.resolver.testexample;

            public class MethodCall_64 {

                static class B {
                    void set(Double d) {
                    }
                }

                B b;

                Double getDoubleValueFromString(String val) {
                    return Double.valueOf(val);
                }

                void method(String s) {
                    b.set(getDoubleValueFromString(s) * 100);
                }
            }
            """;

    @Test
    public void test8() {
        javaInspector.parse(INPUT8);
    }

    @Language("java")
    private static final String INPUT9 = """
            package org.e2immu.analyser.resolver.testexample;

            public class MethodCall_63 {

                interface I {
                }

                void set(I[] is) {
                }

                // from org.apache.commons.lang3
                @SafeVarargs
                static <T> T[] addAll(final T[] array1, final T... array2) {
                    return null;
                }

                void method(I i, I data[]) {
                    set(addAll(new I[]{i}, data));
                }
            }
            """;

    @Test
    public void test9() {
        javaInspector.parse(INPUT9);
    }

    @Language("java")
    private static final String INPUT10 = """
            package org.e2immu.analyser.resolver.testexample;

            public class MethodCall_60 {

                static class ArrayList<I> extends java.util.ArrayList<I> {
                }

                ArrayList<String[]> list;

                String accept(String[][] nameValuePairs) {
                    return nameValuePairs.length + "?";
                }

                String method() {
                    String[][] a = new String[list.size()][2];
                    return accept(list.toArray(a));
                }
            }
            """;

    @Test
    public void test10() {
        javaInspector.parse(INPUT10);
    }
}
