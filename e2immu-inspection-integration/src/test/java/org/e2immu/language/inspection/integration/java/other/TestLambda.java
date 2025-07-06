package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.expression.Lambda;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestLambda extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            import java.util.List;
            import java.util.Set;
            import java.util.stream.Stream;
            class C {
                List<String> method1(List<String> list) {
                    Stream<String> v = list.stream();
                    Stream<String> s2 = v.filter(s -> !s.isEmpty());
                    Stream<String> s3 = s2.filter(s -> s.charAt(0) == 1);
                    return s3.toList();
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        MethodInfo method1 = typeInfo.findUniqueMethod("method1", 1);
        if (method1.methodBody().statements().get(1) instanceof LocalVariableCreation lvc
            && lvc.localVariable().assignmentExpression() instanceof MethodCall mc
            && mc.parameterExpressions().getFirst() instanceof Lambda lambda) {
            MethodInfo mi = lambda.methodInfo();
            assertEquals("7-43:7-54", mi.methodBody().source().compact2());
            assertTrue(mi.isSynthetic());
            ParameterInfo s = mi.parameters().getFirst();
            assertFalse(s.isSynthetic());
            assertEquals("7-38:7-41", s.source().compact2());
            assertEquals("s", s.name());
            TypeInfo ti = mi.typeInfo();
            assertEquals("C.$0", ti.fullyQualifiedName());
            assertEquals(1, ti.interfacesImplemented().size());
            assertEquals("java.util.function.Predicate<String>", ti.interfacesImplemented().getFirst().fullyQualifiedName());
        } else fail();
    }


    @Language("java")
    private static final String INPUT2 = """
            import java.util.List;
            import java.util.Set;
            import java.util.stream.Stream;
            class C {
                void method1(List<String> list) {
                    list.stream().findFirst().ifPresent(s -> {
                        if(s.length() > 10) {
                            throw new RuntimeException();
                        }
                    });
                }
            }
            """;

    @Test
    public void test2() {
        javaInspector.parse(INPUT2);
    }

    @Language("java")
    private static final String INPUT2b = """
            import java.util.List;
            import java.util.Set;
            import java.util.stream.Stream;
            class C {
                void method1(List<String> list) {
                    list.stream().findFirst().ifPresent(s -> {
                        int length = s.length();
                        if(length > 10) {
                            String msg = "too long! "+length;
                            throw new RuntimeException(msg);
                        }
                    });
                }
            }
            """;

    @DisplayName("ParseLambdaExpression.recursiveComputeIsVoid")
    @Test
    public void test2b() {
        javaInspector.parse(INPUT2b);
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            class C {
                interface A { void accept(int a, int b) {} }
                void method1() {
                    m((s, t)->System.out.println(s + " = " + t));
                }
                void m(A a) {
                    // do sth
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = javaInspector.parse(INPUT3);
        MethodInfo method1 = typeInfo.findUniqueMethod("method1", 0);
        MethodCall callM = (MethodCall) method1.methodBody().statements().getFirst().expression();
        Lambda lambda = (Lambda) callM.parameterExpressions().getFirst();
        MethodInfo lambdaMethod = lambda.methodInfo();
        ParameterInfo s = lambda.parameters().getFirst();
        assertEquals("s", s.name());
        assertEquals("5-12:5-12", s.source().compact2());
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            class C {
                interface A { String accept(int a, int b) {} }
                A a = (a, b) -> {
                  System.out.println(a + " = " + b);
                  return a + " = " + b;
                };
            }
            """;

    @Test
    public void test4() {
        javaInspector.parse(INPUT4);

    }


    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            import java.util.function.Consumer;class C {
                class ProxyFactory {
                    public ProxyFactory() { }
                    public void process() { }
                    public static <T> ProxyBuilderImpl<T> builder(Class<T> type) {
                        return new ProxyBuilderImpl<>(type);
                    }
                }
            
                class ProxyBuilderImpl<T> {
                    ProxyBuilderImpl(Class<T> clazz) {
                    }
                    ProxyBuilderImpl<T> customizer(Consumer<T> consumer) { return this; }
                    ProxyFactory build() { return new ProxyFactory(); }
                }
            
                <T> create(Class<T> clazz, String url, Consumer<T> customizer, boolean b, int k) {
                    return ProxyFactory.builder(clazz)
                            .customizer(c -> {
                                switch(k) {
                                    case 1 -> { System.out.println(k); }
                                    case 2-> System.out.println(k+1);
                                    default -> System.out.println("?");
                                }
                            })
                            .customizer(c -> {
                                if(b) {
                                    for(int i = 0; i < k.length; i += 2) {
                                       System.out.println("?");
                                    }
                                }
                            })
                            .build();
                }
            }
            """;

    @Test
    public void test5() {
        javaInspector.parse(INPUT5);
    }

}
