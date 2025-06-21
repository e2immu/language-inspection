package org.e2immu.language.inspection.integration.java.type;

import org.e2immu.language.cst.api.element.Source;
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
            assertSame(QualificationState.FULLY_QUALIFIED, qualifyingTypeSimpleName(typeExpression.source(), pt).qs);
        } else fail();
        {
            MethodInfo make1 = typeInfo.findUniqueMethod("make1", 0);
            ConstructorCall cc = (ConstructorCall) make1.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z", pt.detailedString());
            QualificationData qd1 = qualifyingTypeSimpleName(cc.source(), cc.parameterizedType());
            assertSame(QualificationState.QUALIFIED, qd1.qs);
            assertSame(typeInfo, qd1.qualifier);
        }
        {
            MethodInfo make1 = typeInfo.findUniqueMethod("make3", 0);
            ConstructorCall cc = (ConstructorCall) make1.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z", pt.detailedString());
            QualificationData qd1 = qualifyingTypeSimpleName(cc.source(), cc.parameterizedType());
            assertSame(QualificationState.SIMPLE, qd1.qs);
        }
        {
            MethodInfo make1 = typeInfo.findUniqueMethod("make4", 0);
            ConstructorCall cc = (ConstructorCall) make1.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z", pt.detailedString());
            QualificationData qd1 = qualifyingTypeSimpleName(cc.source(), cc.parameterizedType());
            assertSame(QualificationState.FULLY_QUALIFIED, qd1.qs);
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
            QualificationData qd1 = qualifyingTypeSimpleName(cc.source(), cc.parameterizedType());
            assertSame(QualificationState.QUALIFIED, qd1.qs);
            assertSame(typeInfo, qd1.qualifier);
        }
        {
            MethodInfo make1 = typeInfo.findUniqueMethod("make3", 0);
            ConstructorCall cc = (ConstructorCall) make1.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z<X>", pt.detailedString());
            QualificationData qd1 = qualifyingTypeSimpleName(cc.source(), cc.parameterizedType());
            assertSame(QualificationState.SIMPLE, qd1.qs);
        }
        {
            MethodInfo make1 = typeInfo.findUniqueMethod("make4", 0);
            ConstructorCall cc = (ConstructorCall) make1.methodBody().statements().getFirst().expression();
            ParameterizedType pt = cc.parameterizedType();
            assertEquals("a.b.X.Y.Z", pt.detailedString());
            QualificationData qd1 = qualifyingTypeSimpleName(cc.source(), cc.parameterizedType());
            assertSame(QualificationState.FULLY_QUALIFIED, qd1.qs);
        }
    }


    enum QualificationState {
        SIMPLE, QUALIFIED, FULLY_QUALIFIED;
    }

    record QualificationData(QualificationState qs, TypeInfo qualifier) {
    }

    static QualificationData qualifyingTypeSimpleName(Source source, ParameterizedType pt) {
        Source s = source.detailedSources().detail(pt);
        int len = s.endPos() - s.beginPos() + 1;
        int diff = len - pt.typeInfo().simpleName().length();
        if (diff == 0) return new QualificationData(QualificationState.SIMPLE, null);
        return remainderQualification(diff - 1, pt.typeInfo()); // -1 to remove the dot
    }

    private static QualificationData remainderQualification(int diff, TypeInfo typeInfo) {
        assert diff > 0;
        if (typeInfo.compilationUnitOrEnclosingType().isLeft()) {
            // the rest must be package
            return new QualificationData(QualificationState.FULLY_QUALIFIED, null);
        }
        TypeInfo enclosing = typeInfo.compilationUnitOrEnclosingType().getRight();
        int diff2 = diff - enclosing.simpleName().length();
        if (diff2 == 0) {
            return new QualificationData(QualificationState.QUALIFIED, enclosing);
        }
        return remainderQualification(diff2 - 1, enclosing);
    }
}
