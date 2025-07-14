package org.e2immu.language.inspection.integration.java.type;

import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestFullyQualified extends CommonTest {

    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            class X {
                void method() {
                   java.lang.System.out.println("?");
                }
                static class Y {
                    static class Z {
            
                    }
                }
                static Z make1() {
                    return new X.Y.Z();
                }
                static Z make2() {
                    return new Y.Z();
                }
                static Z make3() {
                    return new Z();
                }
                static Z make4() {
                    return new a.b.X.Y.Z();
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1, JavaInspectorImpl.DETAILED_SOURCES);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 0);
        MethodCall methodCall = (MethodCall) methodInfo.methodBody().statements().getFirst().expression();
        if (methodCall.object() instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr) {
            TypeExpression typeExpression = (TypeExpression) fr.scope();
            ParameterizedType pt = typeExpression.parameterizedType();
            assertEquals("System", pt.detailedString());
            assertEquals("4-8:4-23", typeExpression.source().detailedSources().detail(pt).compact2());
            // FIXME
        } else fail();
        {
            MethodInfo make1 = typeInfo.findUniqueMethod("make1", 0);
            ConstructorCall cc = (ConstructorCall) make1.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z", pt.detailedString());
            // FIXME
        }
        {
            MethodInfo make2 = typeInfo.findUniqueMethod("make2", 0);
            ConstructorCall cc = (ConstructorCall) make2.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z", pt.detailedString());
            // FIXME
        }
        {
            MethodInfo make3 = typeInfo.findUniqueMethod("make3", 0);
            ConstructorCall cc = (ConstructorCall) make3.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z", pt.detailedString());
            // FIXME
        }
        {
            MethodInfo make1 = typeInfo.findUniqueMethod("make4", 0);
            ConstructorCall cc = (ConstructorCall) make1.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z", pt.detailedString());
            // FIXME
        }
    }


    @Language("java")
    public static final String INPUT2 = """
            package a.b;
            class X {
                static class Y {
                    static class Z<T> {
                    }
                }
                static Z make1() {
                    return new X.Y.Z<String>();
                }
                static Z make2() {
                    return new Y.Z<X>();
                }
                static Z make3() {
                    return new Z();
                }
                static Z make4() {
                    return new a.b.X.Y.Z();
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2, JavaInspectorImpl.DETAILED_SOURCES);
        {
            MethodInfo make1 = typeInfo.findUniqueMethod("make1", 0);
            ConstructorCall cc = (ConstructorCall) make1.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z<String>", pt.detailedString());
            // FIXME
        }
        {
            MethodInfo make2 = typeInfo.findUniqueMethod("make2", 0);
            ConstructorCall cc = (ConstructorCall) make2.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z<a.b.X>", pt.detailedString());
            // FIXME
        }
        {
            MethodInfo make3 = typeInfo.findUniqueMethod("make3", 0);
            ConstructorCall cc = (ConstructorCall) make3.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z", pt.detailedString());
            // FIXME
        }
        {
            MethodInfo make1 = typeInfo.findUniqueMethod("make4", 0);
            ConstructorCall cc = (ConstructorCall) make1.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z", pt.detailedString());
            // FIXME
        }
    }


    @Language("java")
    public static final String INPUT3 = """
            package a.b;
            class X {
                @Docstring(embedding = "1, 2, 3")
                void method() {
                   java.lang.System.out.println("?");
                }
                @X.Docstring(embedding = "1, 2, 3")
                void method2() {
                   java.lang.System.out.println("?");
                }
                @a.b.X.Docstring(embedding = "1, 2, 3")
                void method3() {
                   java.lang.System.out.println("?");
                }
                public @interface Docstring {
                    String embedding();
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = javaInspector.parse(INPUT3, new JavaInspectorImpl.ParseOptionsBuilder()
                .setDetailedSources(true).build());
        {
            MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 0);
            AnnotationExpression sw = methodInfo.annotations().getFirst();
            // FIXME
        }
        {
            MethodInfo methodInfo = typeInfo.findUniqueMethod("method2", 0);
            AnnotationExpression sw = methodInfo.annotations().getFirst();
            // FIXME
        }
        {
            MethodInfo methodInfo = typeInfo.findUniqueMethod("method3", 0);
            AnnotationExpression sw = methodInfo.annotations().getFirst();
            // FIXME
        }
    }


    @Language("java")
    public static final String INPUT3b = """
            package a.b;
            class X {
                @Docstring
                void method() {
                   java.lang.System.out.println("?");
                }
                @X.Docstring
                void method2() {
                   java.lang.System.out.println("?");
                }
                @a.b.X.Docstring
                void method3() {
                   java.lang.System.out.println("?");
                }
                public @interface Docstring {
                }
            }
            """;

    @Test
    public void test3b() {
        TypeInfo typeInfo = javaInspector.parse(INPUT3b, new JavaInspectorImpl.ParseOptionsBuilder()
                .setDetailedSources(true).build());
        {
            MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 0);
            AnnotationExpression sw = methodInfo.annotations().getFirst();
            // FIXME
        }
        {
            MethodInfo methodInfo = typeInfo.findUniqueMethod("method2", 0);
            AnnotationExpression sw = methodInfo.annotations().getFirst();
            // FIXME
        }
        {
            MethodInfo methodInfo = typeInfo.findUniqueMethod("method3", 0);
            AnnotationExpression sw = methodInfo.annotations().getFirst();
            // FIXME
        }
    }



    @Language("java")
    public static final String INPUT4 = """
            class X {
                  interface I {
                      interface J {
                          int get();
                      }
                  }
                  static class II implements I {
                      int method0(X.II.J j) {
                          return j.get();
                      }
                      int method1(II.J j) {
                          return j.get();
                      }
                      int method2(I.J j) {
                          return j.get();
                      }
                      int method3(J j) {
                          return j.get();
                      }
                  }
              }
            """;

    @Test
    public void test4() {
        TypeInfo typeInfo = javaInspector.parse(INPUT4, JavaInspectorImpl.DETAILED_SOURCES);
        TypeInfo II = typeInfo.findSubType("II");
        {
            MethodInfo methodInfo = II.findUniqueMethod("method3", 1);
            ParameterInfo first = methodInfo.parameters().getFirst();
            ParameterizedType pt = first.parameterizedType();
            // FIXME
        }
        {
            MethodInfo methodInfo = II.findUniqueMethod("method2", 1);
            ParameterInfo first = methodInfo.parameters().getFirst();
            ParameterizedType pt = first.parameterizedType();
            // FIXME
        }
        {
            MethodInfo methodInfo = II.findUniqueMethod("method1", 1);
            ParameterInfo first = methodInfo.parameters().getFirst();
            ParameterizedType pt = first.parameterizedType();
            // FIXME
        }
        {
            MethodInfo methodInfo = II.findUniqueMethod("method0", 1);
            ParameterInfo first = methodInfo.parameters().getFirst();
            ParameterizedType pt = first.parameterizedType();
            // FIXME
        }
    }

}
