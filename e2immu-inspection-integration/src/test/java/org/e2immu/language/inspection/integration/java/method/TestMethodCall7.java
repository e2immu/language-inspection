package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.cst.api.expression.BinaryOperator;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.TryStatement;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.FileSystem;

import static org.junit.jupiter.api.Assertions.*;

public class TestMethodCall7 extends CommonTest {
    @Language("java")
    private static final String INPUT0 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import org.e2immu.language.inspection.integration.java.importhelper.b.B;
            
            import static org.e2immu.language.inspection.integration.java.importhelper.b.C.doNothing;
            
            public class MethodCall_70 {
            
                B method1(B b) {
                    return b.doNothing();
                }
            
                B method2() {
                    return doNothing();
                }
            }
            """;

    @Test
    public void test0() {
        javaInspector.parse(INPUT0);
    }

    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.analyser.resolver.testexample;
            
            public class MethodCall_71 {
            
                public void b1(int j) {
                    System.out.println(j + " = j");
                    System.out.println("j = " + j);
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo mc71 = javaInspector.parse(INPUT1);
        MethodInfo b1 = mc71.findUniqueMethod("b1", 1);
        for (int i = 0; i < 2; i++) {
            ExpressionAsStatement eas0 = (ExpressionAsStatement) b1.methodBody().statements().get(i);
            MethodCall mc = (MethodCall) eas0.expression();
            Expression p0 = mc.parameterExpressions().get(0);
            assertInstanceOf(BinaryOperator.class, p0);
            assertEquals("Type String", p0.parameterizedType().toString());
            assertEquals("java.io.PrintStream.println(String)", mc.methodInfo().fullyQualifiedName());
        }
    }

    @Language("java")
    private static final String INPUT4 = """
            package org.e2immu.analyser.resolver.testexample;
            
            public class MethodCall_74 {
            
                void init(long[] l1, long[] l2) {
            
                }
            
                void method() {
                    init(null, null);
                }
            }
            """;

    @Test
    public void test4() {
        javaInspector.parse(INPUT4);
    }


    @Language("java")
    private static final String INPUT5 = """
            package org.e2immu.analyser.resolver.testexample;
            
            public class MethodCall_75 {
            
               String method(Class<?> clazz) {
                    Class[] classes = clazz.getInterfaces();
                    for(Class c: classes) {
                        System.out.println(c);
                    }
               }
            }
            """;

    @Test
    public void test5() {
        TypeInfo ti = javaInspector.parse(INPUT5);
        MethodInfo methodInfo = ti.findUniqueMethod("method", 1);
        LocalVariableCreation lvc = (LocalVariableCreation) methodInfo.methodBody().statements().get(0);
        LocalVariable classes = lvc.localVariable();
        ParameterizedType classesPt = classes.parameterizedType();
        assertEquals("Class[]", classesPt.fullyQualifiedName());
        MethodCall getInterfaces = (MethodCall) classes.assignmentExpression();
        ParameterizedType methodRt = getInterfaces.methodInfo().returnType();
        assertEquals("Type Class<?>[]", methodRt.toString());
        assertEquals("Type Class<?>[]", getInterfaces.parameterizedType().toString());
    }


    @Language("java")
    private static final String INPUT6 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.net.URI;
            import java.nio.file.*;
            import java.util.Collections;
            
            public class MethodCall_75 {
            
               String method(URI uri) {
                   try (FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap())) {
                        Path basePath = fileSystem.getPath(".").toAbsolutePath();
                        if (!Files.exists(basePath)) {
                            // called in a privileged action, thus no need to deal with security manager
                            basePath = fileSystem.getPath("agent/.").toAbsolutePath();
                        }
                        return basePath.toString();
                   }
               }
            }
            """;

    @DisplayName("empty varargs")
    @Test
    public void test6() {
        TypeInfo fileSystem = javaInspector.runtime().getFullyQualified(FileSystem.class, true);
        MethodInfo getPath = fileSystem.findUniqueMethod("getPath", 2);
        ParameterInfo pi1 = getPath.parameters().get(1);
        assertTrue(pi1.isVarArgs());

        TypeInfo ti = javaInspector.parse(INPUT6);
        MethodInfo methodInfo = ti.findUniqueMethod("method", 1);
        TryStatement ts = (TryStatement) methodInfo.methodBody().statements().get(0);
        assertEquals(1, ts.resources().size());
        LocalVariableCreation lvc = (LocalVariableCreation) ts.resources().get(0);
        MethodCall nfs = (MethodCall) lvc.localVariable().assignmentExpression();
        Expression arg1 = nfs.parameterExpressions().get(1);
        assertEquals("Collections.emptyMap()", arg1.toString()); // TODO have no type parameters on method call (yet)
    }


    @Language("java")
    private static final String INPUT7 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.util.List;
            import java.util.Map;
            import java.util.function.Function;
            import java.util.stream.Collectors;
            
            public class MethodCall_76 {
            
                interface J {
                    void someMethod();
                }
                interface I {
                    List<J> js();
                    String getName();
                }
                class II implements I {
                    private final String name;
                    II(String name) { this.name = name; }
                    public String getName() { return name; }
                    public List<J> js() { return List.of(); }
                }
            
                Map<String, I> method(List<I> is) {
                    return is.stream().collect(Collectors.toMap(i -> i.getName(), i -> i,
                        (i1, i2)-> new II(i1.getName())));
                }
            
                Map<String, I> method2(List<I> is) {
                    return is.stream().collect(Collectors.toMap(I::getName, Function.identity(),
                        (i1, i2)-> new II(i1.getName())));
                }
            
            }
            """;

    @DisplayName("type forwarding")
    @Test
    public void test7() {
        TypeInfo ti = javaInspector.parse(INPUT7);
        MethodInfo methodInfo = ti.findUniqueMethod("method", 1);
        assertEquals("Type java.util.Map<String,org.e2immu.analyser.resolver.testexample.MethodCall_76.I>",
                methodInfo.methodBody().statements().getFirst().expression().parameterizedType().toString());
    }
}
