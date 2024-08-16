package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.cst.api.expression.BinaryOperator;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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
    private static final String INPUT1 = """
            package org.e2immu.analyser.resolver.testexample;

            public class MethodCall_71 {

                public void b1(int j) {
                    System.out.println(j + " = j");
                    System.out.println("j = " + j);
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo mc71 = javaInspector.parse(INPUT1);
        MethodInfo b1 = mc71.findUniqueMethod("b1", 1);
        for (int i = 0; i < 2; i++) {
            ExpressionAsStatement eas0 = (ExpressionAsStatement) b1.methodBody().statements().get(i);
            MethodCall mc = (MethodCall) eas0.expression();
            Expression p0 = mc.parameterExpressions().get(0);
            assertInstanceOf(BinaryOperator.class, p0);
            assertEquals("Type String", p0.parameterizedType().toString());
            assertEquals("java.io.PrintStream.println(String)", mc.methodInfo().fullyQualifiedName());
        }
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
