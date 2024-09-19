package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

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
        javaInspector.parse(INPUT2);
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
        javaInspector.parse(INPUT7);
    }

}
