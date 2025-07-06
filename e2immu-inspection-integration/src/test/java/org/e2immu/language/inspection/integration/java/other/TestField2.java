package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.expression.BinaryOperator;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestField2 extends CommonTest2 {


    @Language("java")
    String PARENT = """
            package a.b;
            public class Parent {
                public static final String FIELD = "abc";
            }
            """;

    @Language("java")
    String CHILD = """
            package a.b;
            public class Child extends Parent {
                private class Create {
                    String newString(String k) {
                        return k + FIELD;
                    }
                }
            }
            """;

    @Test
    public void testImport() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.b.Parent", PARENT, "a.b.Child", CHILD);
        ParseResult pr1 = init(sourcesByFqn);
        TypeInfo child = pr1.findType("a.b.Child");
        TypeInfo create = child.findSubType("Create");
        MethodInfo newString = create.findUniqueMethod("newString", 1);
        BinaryOperator bo = (BinaryOperator) newString.methodBody().lastStatement().expression();
        if (bo.rhs() instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr) {
            assertEquals("a.b.Parent.FIELD", fr.fullyQualifiedName());
        } else fail();
    }
}
