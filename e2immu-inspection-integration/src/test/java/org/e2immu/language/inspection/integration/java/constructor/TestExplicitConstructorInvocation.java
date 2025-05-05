package org.e2immu.language.inspection.integration.java.constructor;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExplicitConstructorInvocation;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        TypeInfo C1 = typeInfo.findSubType("C1");
        MethodInfo C1Constructor1 = C1.findConstructor(1);

        TypeInfo C2 = typeInfo.findSubType("C2");
        MethodInfo C2Constructor1 = C2.findConstructor(1);
        Statement first = C2Constructor1.methodBody().statements().getFirst();
        if (first instanceof ExplicitConstructorInvocation eci) {
            assertTrue(eci.isSuper());
            assertSame(C1Constructor1, eci.methodInfo());
        }

        TypeInfo C3 = typeInfo.findSubType("C3");
        MethodInfo C3Constructor = C3.findConstructor(1);
        Statement firstC3 = C3Constructor.methodBody().statements().getFirst();
        if (firstC3 instanceof ExplicitConstructorInvocation eci) {
            assertTrue(eci.isSuper());
            assertSame(C2Constructor1, eci.methodInfo());
        }
    }
}
