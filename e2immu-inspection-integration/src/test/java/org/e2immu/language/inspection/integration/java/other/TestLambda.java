package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.expression.BinaryOperator;
import org.e2immu.language.cst.api.expression.Lambda;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.IfElseStatement;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestLambda extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            import java.util.List;
            import java.util.Set;
            import java.util.stream.Stream;
            class C {
                List<String> method1(List<String> list) {
                    Stream<String> v = list.stream();
                    Stream<String> s2 = v.filter(s -> !s.isEmpty());
                    Stream<String> s3 = s2.filter(s -> s.charAt(0) == 1);
                    return s3.toList();
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        MethodInfo method1 = typeInfo.findUniqueMethod("method1", 1);
        if (method1.methodBody().statements().get(1) instanceof LocalVariableCreation lvc
            && lvc.localVariable().assignmentExpression() instanceof MethodCall mc
            && mc.parameterExpressions().get(0) instanceof Lambda lambda) {
            MethodInfo mi = lambda.methodInfo();
            TypeInfo ti = mi.typeInfo();
            assertEquals("C.$1", ti.fullyQualifiedName());
            assertEquals(1, ti.interfacesImplemented().size());
            assertEquals("java.util.function.Predicate<String>", ti.interfacesImplemented().get(0).fullyQualifiedName());
        } else fail();
    }
}
