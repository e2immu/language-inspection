package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;

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

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;

            import org.e2immu.analyser.resolver.testexample.a.Resource;
            import org.e2immu.analyser.resolver.testexample.a.Resources;

            @Resources({
                    @Resource(name = "xx", lookup = "yy", type = java.util.TreeMap.class),
                    @Resource(name = "zz", type = java.lang.Integer.class)
            })
            public class Annotations_1 {
                static final String XX = "xx";
            }
            """;

    @Language("java")
    private static final String INPUT3 = """
            package org.e2immu.analyser.resolver.testexample;

            import org.e2immu.analyser.resolver.testexample.a.Resource;
            import org.e2immu.analyser.resolver.testexample.a.Resources;

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

    @Language("java")
    private static final String INPUT4 = """
            package org.e2immu.analyser.resolver.testexample;

            import org.e2immu.analyser.resolver.testexample.a.Resource;

            import static org.e2immu.analyser.resolver.testexample.Annotations_3.XX;

            @Resource(name = XX, lookup = Annotations_3.ZZ, type = java.util.TreeMap.class)
            public class Annotations_3 {
                static final String XX = "xx";
                static final String ZZ = "zz";
            }
            """;

    @Language("java")
    private static final String INPUT5 = """
            package org.e2immu.analyser.resolver.testexample;

            import org.e2immu.analyser.resolver.testexample.a.Resource;

            import static org.e2immu.analyser.resolver.testexample.Annotations_4.XX;

            @Resource(name = XX, lookup = Annotations_4.ZZ, authenticationType = Resource.AuthenticationType.CONTAINER)
            public class Annotations_4 {
                static final String XX = "xx";
                static final String ZZ = "zz";
            }
            """;
    
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
}
