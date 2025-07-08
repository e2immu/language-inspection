package org.e2immu.language.inspection.integration.java.type;

import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.TypeParameter;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTypeParameter extends CommonTest {

    @Language("java")
    public static final String INPUT5 = """
            package a.b;
            import org.e2immu.annotation.Container;
            import org.e2immu.annotation.Independent;
            class X {
              class Class$<@Independent @Container T> {
            
              }
            }
            """;

    @Test
    public void test5() {
        TypeInfo typeInfo = javaInspector.parse(INPUT5);
        TypeInfo clazz = typeInfo.findSubType("Class$");
        TypeParameter tp = clazz.typeParameters().getFirst();
        assertEquals(2, tp.annotations().size());
        assertEquals("""
                package a.b;
                import org.e2immu.annotation.Container;
                import org.e2immu.annotation.Independent;
                class X { class Class$<@Independent @Container T> { } }
                """, javaInspector.print2(typeInfo));
    }


    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            
            class X {
               static class ArrayList<T> extends java.util.ArrayList<T> {
                   // no need for anything here
               }
               static class I {
                   int k;
               }
            
               static void set(ArrayList<I[]> iArrayList, int i, int j, int k) {
                   iArrayList.get(i)[j].k = k;
               }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        MethodInfo set = typeInfo.findUniqueMethod("set", 4);
        Expression expression = set.methodBody().lastStatement().expression();
        assertEquals("iArrayList.get(i)[j].k=k", expression.toString());
        if (expression instanceof Assignment a) {
            assertEquals("a.b.X.I.k#`12-8`[a.b.X.set(a.b.X.ArrayList<a.b.X.I[]>,int,int,int):2:j]",
                    a.variableTarget().fullyQualifiedName());
        }
    }

}
