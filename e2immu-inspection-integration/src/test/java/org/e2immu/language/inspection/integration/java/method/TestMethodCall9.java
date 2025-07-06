package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.cst.api.expression.Lambda;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
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
}
