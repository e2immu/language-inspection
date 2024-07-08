package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.expression.BinaryOperator;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.IfElseStatement;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

public class TestOperators extends CommonTest {
    /*
   want to ensure that == is an object equals, not an int equals.
    */
    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.HashMap;
            import java.util.Map;

            public class Basics_9 {
                private final Map<String, Integer> map = new HashMap<>();

                public int method1(String k) {
                    Integer v = map.get(k);
                    int r;
                    if (v == null) {
                        int newValue = k.length();
                        map.put(k, newValue);
                        r = newValue;
                    } else {
                        r = v;
                    }
                    return r;
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        MethodInfo method1 = typeInfo.findUniqueMethod("method1", 1);
        if (method1.methodBody().statements().get(2) instanceof IfElseStatement ifElse
            && ifElse.expression() instanceof BinaryOperator bo) {
            assertSame(javaInspector.runtime().equalsOperatorObject(), bo.operator());
        } else fail();
    }
}
