package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
