package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.cst.api.expression.Lambda;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class TestMethodCall2 extends CommonTest {

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


    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.*;

            public class InspectionGaps_2 {
                private static final Map<String, Integer> PRIORITY = new HashMap<>();

                static {
                    PRIORITY.put("e2container", 1);
                    PRIORITY.put("e2immutable", 2);
                }

                static {
                    PRIORITY.put("e1container", 3);
                    PRIORITY.put("e1immutable", 4);
                }

                private static int priority(String in) {
                    return PRIORITY.getOrDefault(in.substring(0, in.indexOf('-')), 10);
                }

                private static String highestPriority(String[] annotations) {
                    List<String> toSort = new ArrayList<>(Arrays.asList(annotations));
                    toSort.sort(Comparator.comparing(InspectionGaps_2::priority));
                    return toSort.get(0);
                }
            }
            """;

    // more of a method call test
    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2);
    }

}
