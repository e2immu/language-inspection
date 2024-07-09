package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

public class TestMethodCall7 extends CommonTest {
    @Language("java")
    private static final String INPUT0 = """
            package org.e2immu.analyser.resolver.testexample;

            import org.e2immu.language.inspection.integration.java.importhelper.b.B;

            import static org.e2immu.language.inspection.integration.java.importhelper.b.C.doNothing;

            public class MethodCall_70 {

                B method1(B b) {
                    return b.doNothing();
                }

                B method2() {
                    return doNothing();
                }
            }
            """;

    @Test
    public void test0() {
        javaInspector.parse(INPUT0);
    }

    @Language("java")
    private static final String INPUT4 = """
            package org.e2immu.analyser.resolver.testexample;

            public class MethodCall_74 {

                void init(long[] l1, long[] l2) {

                }

                void method() {
                    init(null, null);
                }
            }
            """;

    @Test
    public void test4() {
        javaInspector.parse(INPUT4);
    }

}
