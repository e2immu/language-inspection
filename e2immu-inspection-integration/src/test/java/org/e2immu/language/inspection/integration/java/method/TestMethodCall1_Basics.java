package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestMethodCall1_Basics extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package org.e2immu.test;

            import java.util.ArrayList;import java.util.List;

            public class MethodCall_0 {

                private List<String> list;

                public MethodCall_0() {
                   list = new ArrayList<>();
                }
                public void add(String s) {
                   list.add(s);
                }
                public String get(String s) {
                   return list.get(0);
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT);
        FieldInfo fieldInfo = typeInfo.getFieldByName("list", true);
        assertEquals("java.util.List", fieldInfo.type().typeInfo().fullyQualifiedName());
        MethodInfo add = typeInfo.findUniqueMethod("add", 1);
        if (add.methodBody().statements().get(0) instanceof ExpressionAsStatement eas
            && eas.expression() instanceof MethodCall mc) {
            assertEquals("java.util.List.add(E)", mc.methodInfo().fullyQualifiedName());
        } else fail();

        MethodInfo get = typeInfo.findUniqueMethod("get", 1);
        if (get.methodBody().statements().get(0) instanceof ReturnStatement rs
            && rs.expression() instanceof MethodCall mc) {
            assertEquals("java.util.List.get(int)", mc.methodInfo().fullyQualifiedName());
        } else fail();
    }
}
