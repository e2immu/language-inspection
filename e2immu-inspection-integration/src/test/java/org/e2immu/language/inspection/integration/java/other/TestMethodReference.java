package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

public class TestMethodReference extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.HashMap;
            import java.util.Map;
            import java.util.function.Supplier;

            public class MethodReference_0 {

                private Map<String, Integer> make(Supplier<Map<String, Integer>> supplier) {
                    return supplier.get();
                }

                public void method() {
                    Map<String, Integer> map = make(HashMap::new);
                    map.put("a", 1);
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

            import java.util.Map;
            import java.util.Objects;

            public class MethodReference_1 {

                public boolean method(Map<String, Integer> map) {
                    return map.entrySet().stream()
                            .filter(Objects::nonNull)
                            .anyMatch(e -> e.getKey().length() == 3);
                }
            }
            """;

    @Test
    public void test2() {
        javaInspector.parse(INPUT2);
    }


    @Language("java")
    private static final String INPUT3 = """
            package org.e2immu.analyser.resolver.testexample;

            import org.e2immu.annotation.NotModified;
            import org.e2immu.annotation.NotNull;

            import java.util.HashMap;
            import java.util.Map;
            import java.util.function.Function;

            public class MethodReference_2 {
                private final Map<String, Integer> map = new HashMap<>();

                public MethodReference_2(int i) {
                    map.put("" + i, i);
                }

                public int get(String s) {
                    return map.get(s);
                }

                @NotNull
                @NotModified
                public Function<String, Integer> getFunction() {
                    return map::get;
                }

                @NotNull
                @NotModified
                public Function<String, Integer> getFunction2() {
                    return this::get;
                }

                @NotNull
                @NotModified
                public static Function<String, Integer> getFunction3() {
                    return String::length;
                }

                @NotNull
                @NotModified
                public static Function<String, Integer> getFunction4() {
                    return java.lang.String::length;
                }
            }
            """;

    @Test
    public void test3() {
        javaInspector.parse(INPUT3);
    }


    @Language("java")
    private static final String INPUT4 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.Arrays;
            import java.util.Set;
            import java.util.stream.Collectors;
            import java.util.stream.Stream;

            // test is here because the crash shows in the method reference "contains"; problem is probably in forwarding of type info
            public class MethodReference_3 {
                public static boolean isEmpty(Object[] array) {
                    return array == null || array.length == 0;
                }

                // everything is fine when .parallel is excluded, or the 2nd part of the disjunction is left out
                // k must be a String (T of Stream = String; with parallel in between: = T)
                public Set<String> method(Set<String> keysWithPrefix, String[] pattern) {
                    return keysWithPrefix
                            .stream()
                            .parallel()
                            .filter(k -> isEmpty(pattern) || Arrays.stream(pattern).parallel().anyMatch(k::contains))
                            .collect(Collectors.toSet());
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

            import java.util.Set;
            import java.util.stream.Collectors;

            public class MethodReference_3B {
                public static boolean isEmpty(Object[] array) {
                    return array == null || array.length == 0;
                }


                public Set<String> method(Set<String> keysWithPrefix) {
                    return keysWithPrefix
                            .stream()
                            .parallel()
                            .filter("abc"::contains)
                            .collect(Collectors.toSet());
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

            import java.util.HashMap;
            import java.util.List;
            import java.util.Map;
            import java.util.Objects;
            import java.util.stream.Collectors;
            import java.util.stream.Stream;

            public class MethodReference_3C {

                record R2(long l) {}
                record R1(R2 r2) {
                    long l() {
                        return r2.l();
                    }
                }

                // result of .map(R1::new) is not of correct type
                private Map<Long, R1> method(List<R2> r2) {
                    Map<Long, R1> result = new HashMap<>();
                    if(r2 != null) {
                        result = r2
                                .stream()
                                .filter(Objects::nonNull)
                                .map(R1::new)
                                .filter(r -> Integer.MIN_VALUE != r.l())
                                .collect(Collectors.toMap(R1::l, x -> x));
                    }
                    return result;
                }

                // this one works fine
                private Map<Long, R1> method2(List<R2> r2) {
                    Map<Long, R1> result = new HashMap<>();
                    if(r2 != null) {
                        Stream<R1> r1Stream = r2
                                .stream()
                                .filter(Objects::nonNull)
                                .map(R1::new);
                        result = r1Stream
                                .filter(r -> Integer.MIN_VALUE != r.l())
                                .collect(Collectors.toMap(R1::l, x -> x));
                    }
                    return result;
                }
            }
            """;

    @Test
    public void test6() {
        javaInspector.parse(INPUT6);
    }


    @Language("java")
    private static final String INPUT7 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.List;
            import java.util.Map;

            public class MethodReference_4 {

                private static void biConsumer(String s, List<Integer> list) {
                    System.out.println(s + ": " + list.size());
                }

                public void method(Map<String, List<Integer>> map) {
                    map.forEach(MethodReference_4::biConsumer);
                }

                // ---------
                private static String biFunction(String s1, String s2) {
                    return s1.length() > s2.length() ? s1 : s2;
                }

                private static Map.Entry<String, String> biFunction(Map.Entry<String, String> e1, Map.Entry<String, String> e2) {
                    return e1.getKey().length() > e2.getKey().length() ? e1 : e2;
                }

                public int method2(Map<String, String> map) {
                    return map.entrySet().stream().reduce(MethodReference_4::biFunction).orElseThrow().getKey().length();
                }
            }
            """;

    @Test
    public void test7() {
        javaInspector.parse(INPUT7);
    }
}
