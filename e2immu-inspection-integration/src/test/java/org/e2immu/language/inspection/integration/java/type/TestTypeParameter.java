package org.e2immu.language.inspection.integration.java.type;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.type.TypeParameter;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

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
            package b;
            
            import java.util.List;
            
            class C {
                interface D { }
                interface A {
                    interface B<T> { }
                    List<B<D>> get();
                }
            
                void m(A a) {
                    List<A.B<D>> x = a.get();
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2, JavaInspectorImpl.DETAILED_SOURCES);
        MethodInfo m = typeInfo.findUniqueMethod("m", 1);
        LocalVariableCreation lvc = (LocalVariableCreation) m.methodBody().lastStatement();
        ParameterizedType list = lvc.localVariable().parameterizedType();
        assertEquals("java.util.List<b.C.A.B<b.C.D>>", list.fullyQualifiedName());
        ParameterizedType pt = list.parameters().getFirst();
        assertEquals("13-14:13-19", lvc.source().detailedSources().detail(pt).compact2());

        //noinspection ALL
        List<DetailedSources.Builder.TypeInfoSource> tis = (List<DetailedSources.Builder.TypeInfoSource>)
                lvc.source().detailedSources().associatedObject(pt.typeInfo());
        assertEquals("13-14:13-16", lvc.source().detailedSources().detail(pt.typeInfo()).compact2());
        assertEquals(1, tis.size());
        assertEquals("TypeInfoSource[typeInfo=b.C.A, source=@13:14-13:14]", tis.getFirst().toString());
        // Note that there is no b.C are, the qualification is A.
    }


    @Language("java")
    public static final String INPUT3 = """
            package a.b;
            class A {
                public record B<Z extends A>(Z t) {}
                public record BB<Z extends a.b.A>(Z t) {}
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = javaInspector.parse(INPUT3, JavaInspectorImpl.DETAILED_SOURCES);
        {
            TypeInfo clazz = typeInfo.findSubType("B");
            TypeParameter tp = clazz.typeParameters().getFirst();
            assertEquals("Z=TP#0 in B [Type a.b.A]", tp.toStringWithTypeBounds());
            assertEquals("3-21:3-31", tp.source().compact2());
            ParameterizedType bound = tp.typeBounds().getFirst();
            assertEquals("3-31:3-31", tp.source().detailedSources().detail(bound).compact2());
        }
        {
            TypeInfo clazz = typeInfo.findSubType("BB");
            TypeParameter tp = clazz.typeParameters().getFirst();
            assertEquals("Z=TP#0 in BB [Type a.b.A]", tp.toStringWithTypeBounds());
            assertEquals("4-22:4-36", tp.source().compact2());
            ParameterizedType bound = tp.typeBounds().getFirst();
            assertEquals("4-32:4-36", tp.source().detailedSources().detail(bound).compact2());
            assertEquals("4-32:4-34", tp.source().detailedSources().detail(bound.typeInfo().packageName()).compact2());
        }
    }

}
