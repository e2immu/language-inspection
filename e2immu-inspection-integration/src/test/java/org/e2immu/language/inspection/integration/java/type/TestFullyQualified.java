package org.e2immu.language.inspection.integration.java.type;

import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.TypeExpression;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.e2immu.language.cst.api.info.TypeInfo.QualificationState.*;
import static org.e2immu.language.cst.api.info.TypeInfo.qualifyingTypeSimpleName;
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
        TypeInfo typeInfo = javaInspector.parse(INPUT1, new JavaInspectorImpl.ParseOptionsBuilder().setDetailedSources(true).build());
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 0);
        MethodCall methodCall = (MethodCall) methodInfo.methodBody().statements().getFirst().expression();
        if (methodCall.object() instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr) {
            TypeExpression typeExpression = (TypeExpression) fr.scope();
            ParameterizedType pt = typeExpression.parameterizedType();
            assertEquals("System", pt.detailedString());
            assertEquals("4-8:4-23", typeExpression.source().detailedSources().detail(pt).compact2());
            assertSame(FULLY_QUALIFIED, qualifyingTypeSimpleName(typeExpression.source(), pt).state());
        } else fail();
        {
            MethodInfo make1 = typeInfo.findUniqueMethod("make1", 0);
            ConstructorCall cc = (ConstructorCall) make1.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z", pt.detailedString());
            TypeInfo.QualificationData qd1 = qualifyingTypeSimpleName(cc.source(), cc.parameterizedType());
            assertSame(QUALIFIED, qd1.state());
            assertSame(typeInfo, qd1.qualifier());
        }
        {
            MethodInfo make2 = typeInfo.findUniqueMethod("make2", 0);
            ConstructorCall cc = (ConstructorCall) make2.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z", pt.detailedString());
            TypeInfo.QualificationData qd2 = qualifyingTypeSimpleName(cc.source(), cc.parameterizedType());
            assertSame(QUALIFIED, qd2.state());
            assertSame(typeInfo.findSubType("Y"), qd2.qualifier());
        }
        {
            MethodInfo make3 = typeInfo.findUniqueMethod("make3", 0);
            ConstructorCall cc = (ConstructorCall) make3.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z", pt.detailedString());
            TypeInfo.QualificationData qd3 = qualifyingTypeSimpleName(cc.source(), cc.parameterizedType());
            assertSame(SIMPLE, qd3.state());
        }
        {
            MethodInfo make1 = typeInfo.findUniqueMethod("make4", 0);
            ConstructorCall cc = (ConstructorCall) make1.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z", pt.detailedString());
            TypeInfo.QualificationData qd1 = qualifyingTypeSimpleName(cc.source(), cc.parameterizedType());
            assertSame(FULLY_QUALIFIED, qd1.state());
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
        TypeInfo typeInfo = javaInspector.parse(INPUT2, new JavaInspectorImpl.ParseOptionsBuilder().setDetailedSources(true).build());
        {
            MethodInfo make1 = typeInfo.findUniqueMethod("make1", 0);
            ConstructorCall cc = (ConstructorCall) make1.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z<String>", pt.detailedString());
            TypeInfo.QualificationData qd1 = qualifyingTypeSimpleName(cc.source(), cc.parameterizedType());
            assertSame(QUALIFIED, qd1.state());
            assertSame(typeInfo, qd1.qualifier());
        }
        {
            MethodInfo make2 = typeInfo.findUniqueMethod("make2", 0);
            ConstructorCall cc = (ConstructorCall) make2.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z<a.b.X>", pt.detailedString());
            TypeInfo.QualificationData qd2 = qualifyingTypeSimpleName(cc.source(), cc.parameterizedType());
            assertSame(QUALIFIED, qd2.state());
            assertSame(typeInfo.findSubType("Y"), qd2.qualifier());
        }
        {
            MethodInfo make3 = typeInfo.findUniqueMethod("make3", 0);
            ConstructorCall cc = (ConstructorCall) make3.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z", pt.detailedString());
            TypeInfo.QualificationData qd3 = qualifyingTypeSimpleName(cc.source(), cc.parameterizedType());
            assertSame(SIMPLE, qd3.state());
        }
        {
            MethodInfo make1 = typeInfo.findUniqueMethod("make4", 0);
            ConstructorCall cc = (ConstructorCall) make1.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z", pt.detailedString());
            TypeInfo.QualificationData qd1 = qualifyingTypeSimpleName(cc.source(), cc.parameterizedType());
            assertSame(FULLY_QUALIFIED, qd1.state());
        }
    }

}
