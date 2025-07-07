package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestMethodCall9 extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            
            import org.springframework.util.function.ThrowingFunction;
            import java.lang.reflect.InvocationTargetException;
            import java.util.List;
            import java.util.Objects;
            
            class X {
                interface Element {
                    String getName();
                }
                public static <T> List<T> constructSet(List<?> list, Class<T> clazz, String childName) {
                	return list.stream()
                		.filter(Objects::nonNull)
                		.map(Element.class::cast)
                		.filter(e -> childName.equals(e.getName()))
                		.map(((ThrowingFunction<Element, T>)e -> {
                			try {
                				return clazz.getDeclaredConstructor(Element.class).newInstance(e);
                			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                			    | InvocationTargetException | NoSuchMethodException | SecurityException e1) {
                				throw new RuntimeException(e1);
                			}
                		}))
                		.toList();
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);

    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            
            class X {
                interface I { }
                class C implements I { }
                String method(C c) {
                    return "s";
                }
                String wrap(String t) {
                    return t;
                }
                void use(C c, long id) {
                    wrap(method(find(c, id)));
                }
                static <T extends I> T find(T t, long id) {
                    return t;
                }
            }
            """;

    @Test
    public void test2() {
        javaInspector.parse(INPUT2);
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            
            class X {
                interface I { }
                class C implements I { }
                String method(C c) {
                    return "s";
                }
                String wrap(String t) {
                    return t;
                }
                void use(C c, long id) {
                    wrap(method(id < 0 ? find(c, -id) : find(c, id)));
                }
                static <T extends I> T find(T t, long id) {
                    return t;
                }
            }
            """;

    @DisplayName("added complication: ?: operator")
    @Test
    public void test3() {
        javaInspector.parse(INPUT3);
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            
            class X {
                interface P { }
                record PR() implements P {}
                interface BT { }
                record BTR(int count) implements BT {}
                interface F<T extends BT> {
                    boolean test(T t);
                }
                static <T extends P, B extends BT> T filter(T theSource, T theTarget, F<B> filter) {
                    return theTarget;
                }
                PR method(PR pr) {
                    return X.<PR, BTR>filter(pr, null, b -> b.count == 1);
                }
            }
            """;

    @Test
    public void test4() {
        javaInspector.parse(INPUT4);
    }


    @Language("java")
    private static final String INPUT4b = """
            package a.b;
            
            class X {
                interface P { }
                record PR(int r) implements P {}
                interface F<T extends P> { boolean test(T t); }
                static <B extends P> boolean filter(B b, F<B> filter) {
                    return filter.test(b);
                }
                boolean method(PR pr) {
                    return X.filter(pr, b -> b.r == 1);
                }
                boolean method2(PR pr) {
                    return X.<PR>filter(pr, b -> b.r == 1);
                }
               // boolean method4(PR pr) {
              //      return X.<PR>filter(null, b -> b.r == 1);
               // }
            }
            """;

    @Test
    public void test4b() {
       TypeInfo X = javaInspector.parse(INPUT4b, new JavaInspectorImpl.ParseOptionsBuilder().setDetailedSources(true).build());
        MethodInfo methodInfo = X.findUniqueMethod("method2", 1);
        MethodCall mc2 = (MethodCall) methodInfo.methodBody().lastStatement().expression();
        assertEquals(1, mc2.typeArguments().size());
        ParameterizedType ta1 = mc2.typeArguments().getFirst();
        assertEquals("Type a.b.X.PR", ta1.toString());
        assertEquals("14-19:14-20", mc2.source().detailedSources().detail(ta1).compact2());
    }
}
