package org.e2immu.language.inspection.integration.java.type;

import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
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


    @Language("java")
    public static final String INPUT2 = """
            package a.b;
            
            import java.util.ArrayList;
            import java.util.List;import java.util.concurrent.atomic.AtomicBoolean;
            
            class X {
              int method() {
                 return new ArrayList<AtomicBoolean>().size();
              }
              int method2() {
                 return new ArrayList<>().size();
              }
              int method3() {
                 List<AtomicBoolean> list = new ArrayList<>();
                 return list.size();
              }
              int method4() {
                List<AtomicBoolean> list = new ArrayList();
                return list.size();
              }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2);
        MethodInfo method = typeInfo.findUniqueMethod("method", 0);
        assertEquals("""
                [TypeReference[typeInfo=int, explicit=true], \
                TypeReference[typeInfo=java.util.concurrent.atomic.AtomicBoolean, explicit=true], \
                TypeReference[typeInfo=java.util.ArrayList, explicit=true]]\
                """, method.typesReferenced(true).toList().toString());
        MethodInfo method2 = typeInfo.findUniqueMethod("method2", 0);
        assertEquals("""
                [TypeReference[typeInfo=int, explicit=true], \
                TypeReference[typeInfo=java.util.ArrayList, explicit=true]]\
                """, method2.typesReferenced(true).toList().toString());
        MethodInfo method3 = typeInfo.findUniqueMethod("method3", 0);
        LocalVariableCreation lvc3 = (LocalVariableCreation) method3.methodBody().statements().getFirst();
        assertEquals("""
                [TypeReference[typeInfo=java.util.concurrent.atomic.AtomicBoolean, explicit=false], \
                TypeReference[typeInfo=java.util.ArrayList, explicit=true]]\
                """, lvc3.localVariable().assignmentExpression().typesReferenced().toList().toString());
        MethodInfo method4 = typeInfo.findUniqueMethod("method4", 0);
        LocalVariableCreation lvc4 = (LocalVariableCreation) method4.methodBody().statements().getFirst();
        assertEquals("""
               [TypeReference[typeInfo=java.util.ArrayList, explicit=true]]\
                """, lvc4.localVariable().assignmentExpression().typesReferenced().toList().toString());
    }

}
