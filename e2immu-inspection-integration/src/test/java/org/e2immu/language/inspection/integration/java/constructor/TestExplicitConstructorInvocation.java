package org.e2immu.language.inspection.integration.java.constructor;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

public class TestExplicitConstructorInvocation extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.Map;

            public class ExplicitConstructorInvocation_1 {

                public final Map<String, Integer> map;

                public ExplicitConstructorInvocation_1() {
                    this(Map.of());
                }

                public ExplicitConstructorInvocation_1(Map<String, Integer> map) {
                    this.map = Map.copyOf(map);
                }

            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
    }

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.Map;

            public class ExplicitConstructorInvocation_2 {

                static class C1 {
                    public final Map<String, Integer> map;

                    public C1() {
                        this(Map.of());
                    }

                    public C1(Map<String, Integer> map) {
                        this.map = Map.copyOf(map);
                    }
                }

                static class C2 extends C1 {

                    public C2(Map<String, Integer> map) {
                        super(map);
                    }
                }

                static class C3 extends C2 {

                    public C3() {
                        super(Map.of());
                    }

                    public C3(Map<String, Integer> map) {
                        super(map);
                    }
                }

            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2);
    }
}
