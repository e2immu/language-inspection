package org.e2immu.language.inspection.integration.java.constructor;

import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestAnonymousType extends CommonTest {


    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.List;
            import java.util.function.Predicate;

            public class AnonymousType_0 {

                private String mask;

                public static List<String> method(List<String> list, String mask) {
                    Predicate<String> stringPredicate = new Predicate<String>() {
                        @Override
                        public boolean test(String s) {
                            // must be the 'mask' parameter, not the field!
                            return !s.isEmpty() && s.charAt(0) == mask.charAt(0);
                        }
                    };
                    return list.stream().filter(stringPredicate).toList();
                }

                class NonStatic {
                    @Override
                    public String toString() {
                        // at the same time, the 'mask' field must be accessible
                        return mask;
                    }
                }
            }
            """;


    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        MethodInfo method = typeInfo.findUniqueMethod("method", 2);
        if (method.methodBody().statements().get(0) instanceof LocalVariableCreation lvc
            && lvc.localVariable().assignmentExpression() instanceof ConstructorCall cc) {
            TypeInfo anon = cc.anonymousClass();
            MethodInfo test = anon.findUniqueMethod("test", 1);
            if (test.methodBody().statements().get(0) instanceof ReturnStatement rs
                && rs.expression() instanceof And and
                && and.expressions().get(1) instanceof BinaryOperator eq
                && eq.rhs() instanceof MethodCall mc) {
                Expression object = mc.object();
                if (object instanceof VariableExpression ve) {
                    assertInstanceOf(ParameterInfo.class, ve.variable());
                } else fail();
            } else {
                fail();
            }
        } else fail();
    }

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;


            import java.util.ArrayList;
            import java.util.List;
            import java.util.function.Supplier;

            // copied from Independent1_10; issues with functional interface computation
            public class AnonymousType_1<T> {
                List<T> list = new ArrayList<>();

                @SafeVarargs
                static <T> AnonymousType_1<T> of(T... ts) {
                    AnonymousType_1<T> result = new AnonymousType_1<>();
                    result.fill(new Supplier<>() {
                        int i;

                        @Override
                        public T get() {
                            return i < ts.length ? ts[i++] : null;
                        }
                    });
                    return result;
                }

                private void fill(Supplier<T> supplier) {
                    T t;
                    while ((t = supplier.get()) != null) list.add(t);
                }
            }
            """;


    @Test
    public void test2() {
        javaInspector.parse(INPUT2);
    }
}
