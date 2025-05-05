package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestVar extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.List;
            import java.util.stream.Collectors;
            
            class X {
                interface J {
                    void someMethod();
                }
                interface I {
                    List<J> js();
                }
            
                void methodExplicit(I i) {
                    var list = i.js();
                    var stream = list.stream();
                    var set = stream.collect(Collectors.toUnmodifiableSet());
                }
            
                void method(I i) {
                    var list = i.js();
                    var set = list.stream().collect(Collectors.toUnmodifiableSet());
                    set.forEach(j ->{
                        System.out.println("calling method on j");
                        j.someMethod();
                    });
                }
            }
            """;

    /*
    This test deals with type forwarding, in a context where type forwarding is strictly necessary.
    Issue is in statement 'var set = ...'. While we know the type of list, we don't know which return type to expect.
    The methods are
     - Stream<T> stream() in List<T>
     - <R,A> R collect(Collector<? super T, A, R> collector) in Stream<T>, and
     - static <T> Collector<T,?,Set<T>> toUnmodifiableSet().

     The type flow should go List<T>~List<J> -> Stream<J> -> J~T as argument of collect(...),
     so that <T> in toUnmodifiableSet ~ J, so that R ~ Set<J>, so that 'set' gets type Set<J>.

     Flow:
     in MethodResolution.resolveMethod(), methodName=collect.
        Extra info TP#0 in Stream -> a.b.X.J
        forwardType.type() = null, erasure = false
        but we should be able to use the forward type T~J when evaluating the first argument of 'collect'.
        Candidate.newParameterExpressions.getFirst() should have a concrete return type Collector<J, ?, Set<J>>.
        The call to evaluate Collectors.toUnmodifiableSet() occurs with the forward type Collector<J,A,R>.
        ->
     */
    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        {
            MethodInfo methodInfo = typeInfo.findUniqueMethod("methodExplicit", 1);
            Statement s1 = methodInfo.methodBody().statements().getFirst();
            LocalVariableCreation lvc1 = (LocalVariableCreation) s1;
            assertEquals("Type java.util.List<a.b.X.J>", lvc1.localVariable().parameterizedType().toString());
            Statement s2 = methodInfo.methodBody().statements().get(1);
            LocalVariableCreation lvc2 = (LocalVariableCreation) s2;
            assertEquals("Type java.util.stream.Stream<a.b.X.J>", lvc2.localVariable().parameterizedType().toString());
            Statement s3 = methodInfo.methodBody().statements().get(2);
            LocalVariableCreation lvc3 = (LocalVariableCreation) s3;
            assertEquals("Type java.util.Set<a.b.X.J>", lvc3.localVariable().parameterizedType().toString());
        }
        {
            MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 1);
            Statement s1 = methodInfo.methodBody().statements().getFirst();
            LocalVariableCreation lvc1 = (LocalVariableCreation) s1;
            assertEquals("Type java.util.List<a.b.X.J>", lvc1.localVariable().parameterizedType().toString());
            Statement s2 = methodInfo.methodBody().statements().get(1);
            LocalVariableCreation lvc2 = (LocalVariableCreation) s2;
            assertEquals("Type java.util.Set<a.b.X.J>", lvc2.localVariable().parameterizedType().toString());
        }
    }

}
