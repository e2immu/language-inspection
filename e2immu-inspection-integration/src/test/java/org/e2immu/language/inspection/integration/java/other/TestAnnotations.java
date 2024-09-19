package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.ArrayInitializer;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.StringConstant;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestAnnotations extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.analyser.resolver.testexample;

            import org.e2immu.annotation.ImmutableContainer;

            public class Annotations_0 {

                @ImmutableContainer("false")
                public static boolean method() {
                    return false;
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

            import org.e2immu.language.inspection.integration.java.importhelper.a.Resources;
            import org.e2immu.language.inspection.integration.java.importhelper.a.Resource;

            @Resources({
                    @Resource(name = "xx", lookup = "yy", type = java.util.TreeMap.class),
                    @Resource(name = "zz", type = java.lang.Integer.class)
            })
            public class Annotations_1 {
                static final String XX = "xx";
            }
            """;

    @Test
    public void test2() {
        TypeInfo t = javaInspector.parse(INPUT2);
        assertEquals(1, t.annotations().size());
        AnnotationExpression ae = t.annotations().get(0);
        assertEquals("org.e2immu.language.inspection.integration.java.importhelper.a.Resources",
                ae.typeInfo().fullyQualifiedName());
        assertEquals(1, ae.keyValuePairs().size());
        AnnotationExpression.KV kv0 = ae.keyValuePairs().get(0);
        assertEquals("value", kv0.key());
        assertTrue(kv0.keyIsDefault());
        Expression e = kv0.value();
        if (e instanceof ArrayInitializer ai) {
            if (ai.expressions().get(0) instanceof AnnotationExpression ae1) {
                assertEquals(3, ae1.keyValuePairs().size());
            } else fail();
        } else fail();
    }

    @Language("java")
    private static final String INPUT3 = """
            package org.e2immu.analyser.resolver.testexample;

            import org.e2immu.language.inspection.integration.java.importhelper.a.Resources;
            import org.e2immu.language.inspection.integration.java.importhelper.a.Resource;

            import static org.e2immu.analyser.resolver.testexample.Annotations_2.XX;

            @Resources({
                    @Resource(name = XX, lookup = "yy", type = java.util.TreeMap.class),
                    @Resource(name = Annotations_2.ZZ, type = Integer.class)
            })
            public class Annotations_2 {
                static final String XX = "xx";
                static final String ZZ = "zz";
            }
            """;

    @Test
    public void test3() {
        javaInspector.parse(INPUT3);
    }

    @Language("java")
    private static final String INPUT4 = """
            package org.e2immu.analyser.resolver.testexample;

            import org.e2immu.language.inspection.integration.java.importhelper.a.Resource;
            import static org.e2immu.analyser.resolver.testexample.Annotations_3.XX;

            @Resource(name = XX, lookup = Annotations_3.ZZ, type = java.util.TreeMap.class)
            public class Annotations_3 {
                static final String XX = "xx";
                static final String ZZ = "zz";
            }
            """;

    @Test
    public void test4() {
        javaInspector.parse(INPUT4);
    }

    @Language("java")
    private static final String INPUT5 = """
            package org.e2immu.analyser.resolver.testexample;

            import org.e2immu.language.inspection.integration.java.importhelper.a.Resource;
            import static org.e2immu.analyser.resolver.testexample.Annotations_4.XX;

            @Resource(name = XX, lookup = Annotations_4.ZZ, authenticationType = Resource.AuthenticationType.CONTAINER)
            public class Annotations_4 {
                static final String XX = "xx";
                static final String ZZ = "zz";
            }
            """;

    @Test
    public void test5() {
        javaInspector.parse(INPUT5);
    }

    @Language("java")
    private static final String INPUT6 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.lang.annotation.Retention;
            import java.lang.annotation.Target;

            import static java.lang.annotation.ElementType.FIELD;
            import static java.lang.annotation.RetentionPolicy.RUNTIME;

            @Target({FIELD})
            @Retention(RUNTIME)
            public @interface Annotations_5 {

                Class<?> value();

                String extra() default "!";
            }
            """;

    @Test
    public void test6() {
        javaInspector.parse(INPUT6);
    }


    @Language("java")
    private static final String INPUT7 = """
            import java.io.FileInputStream;
            import java.io.IOException;
            import java.io.ObjectInputStream;
            import java.io.Serializable;

            public class X {

                public static <T extends Serializable> T u(final String pFilename, final Class<T> pClass) {
                    try {
                        final FileInputStream myFileInputStream = new FileInputStream(pFilename);
                        final ObjectInputStream myObjectInputStream = new ObjectInputStream(myFileInputStream);
                        Object myObject = myObjectInputStream.readObject();
                        myObjectInputStream.close();
                        myFileInputStream.close();
                        if (pClass.isInstance(myObject)) {
                            @SuppressWarnings("unchecked")
                            final T mObject = (T) myObject;
                            return mObject;
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        System.err.println("error " + e);
                    }
                    return null;
                }
            }
            """;

    @Test
    public void test7() {
        TypeInfo X = javaInspector.parse(INPUT7);
        MethodInfo u = X.findUniqueMethod("u", 2);
        Statement ifElse = u.methodBody().statements().get(0).block().statements().get(5);
        Statement s = ifElse.block().statements().get(0);
        assertEquals(1, s.annotations().size());
        AnnotationExpression ae = s.annotations().get(0);
        assertEquals(SuppressWarnings.class.getCanonicalName(), ae.typeInfo().fullyQualifiedName());
        AnnotationExpression.KV kv = ae.keyValuePairs().get(0);
        assertEquals("value", kv.key());
        assertEquals("unchecked", ((StringConstant) kv.value()).constant());
    }

}
