package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.cst.api.expression.Lambda;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestMethodCall1 extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.Arrays;
            import java.util.function.Function;
            import java.util.stream.Collectors;

            public class MethodCall_18 {

                interface AnnotationExpression {
                    <T> T extract(String s, T t);
                }

                private final AnnotationExpression ae = new AnnotationExpression() {
                    @Override
                    public <T> T extract(String s, T t) {
                        return s.length() > 0 ? null : t;
                    }
                };

                // int
                public String method1() {
                    Function<AnnotationExpression, String> f1 = ae -> {
                        Integer i = ae.extract("level", 3);
                        return i == null ? null : Integer.toString(i);
                    };
                    return f1.apply(ae);
                }

                // string[]
                public String method2() {
                    Function<AnnotationExpression, String> f2 = ae -> {
                        String[] inspected = ae.extract("to", new String[]{});
                        return Arrays.stream(inspected).sorted().collect(Collectors.joining(","));
                    };
                    return f2.apply(ae);
                }

                // Integer
                public String method3() {
                    Function<AnnotationExpression, String> f3 = ae -> {
                        Integer i = ae.extract("level", null);
                        return Integer.toString(i);
                    };
                    return f3.apply(ae);
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        MethodInfo method2 = typeInfo.findUniqueMethod("method2", 0);
        if (method2.methodBody().statements().get(0) instanceof LocalVariableCreation lvc
            && lvc.localVariable().assignmentExpression() instanceof Lambda lambda) {
            assertEquals("Type java.util.function.Function<org.e2immu.analyser.resolver.testexample.MethodCall_18.AnnotationExpression,String>",
                    lambda.concreteFunctionalType().toString());
            MethodInfo sam = lambda.methodInfo();
            if (sam.methodBody().statements().get(0) instanceof LocalVariableCreation lvc2
                && lvc2.localVariable().assignmentExpression() instanceof MethodCall mc) {
                assertEquals("Type String[]", mc.concreteReturnType().toString());
            } else fail();
        } else fail();
    }

}
