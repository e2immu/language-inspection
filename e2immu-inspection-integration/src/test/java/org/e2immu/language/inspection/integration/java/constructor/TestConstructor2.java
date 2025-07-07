package org.e2immu.language.inspection.integration.java.constructor;

import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestConstructor2 extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            public class Key<K,V> {
              private final K key;
              private final Class<V> clazz;
              private Key(K key, Class<V> clazz){
                this.key = key;
                this.clazz = clazz;
              }
              public static <K,V> Key<K,V> create(K key, Class<V> clazz){
                return new Key<K,V>(key, clazz);
              }
            }
            """;

    @Test
    public void test1() {
        javaInspector.parse(INPUT1);
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            public class X {
              record Pair<F, G>(F f, G g) {
              }
            
              record R<F, G>(Pair<F, G> pair) {
                public R {
                  assert pair != null;
                }
              }
            
              static <X, Y> R<Y, X> reverse7(R<X, Y> r) {
                return new R(new Pair(r.pair.g, r.pair.f));
              }
            }
            """;

    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        MethodInfo reverse7 = X.findUniqueMethod("reverse7", 1);
        ConstructorCall cc = (ConstructorCall) reverse7.methodBody().statements().getFirst().expression();
        assertEquals("Type a.b.X.R<Y,X>", cc.parameterizedType().toString());
        assertEquals(2, cc.parameterizedType().parameters().size());
        ConstructorCall cc2 = (ConstructorCall) cc.parameterExpressions().getFirst();
        assertEquals("Type a.b.X.Pair<Y,X>", cc2.parameterizedType().toString());
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            public class X {
                static class MyException {
                   public MyException(long id, Throwable theCause) {
                   }
                   public MyException(long id, String... args){
                   }
                   public MyException(long id, boolean logTrace, String... args){
                   }
                   public MyException(long id, String[] args, Throwable theCause) {
                   }
                   public MyException(long id, String[] args, Throwable theCause, boolean logTrace) {
                   }
                   public MyException(long id, long[] args) {
                   }
                   public MyException(long id, long[] args, Throwable theCause) {
                   }
                   public MyException(long id, int[] args) {
                   }
                }
                void method(String msg, Exception e) {
                    throw new MyException(3L, new String[] { "a" + msg }, e);
                }
                void method2(String msg) {
                    throw new MyException(3L, new String[] { "a", "b"}, new RuntimeException(msg+" abc"));
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);

    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.Arrays;import java.util.stream.Stream;public class X {
                static class B {
                    B(String s) {}
                    B(String s1, String s2) {}
                    B(String... strings) {}
                    B(B b, String... strings) {}
                }
                static class A extends B {
                   A(String s) { super(mod(s)); }
                   A(String s1, String s2) { super(mod(s1), s2); }
                   A(String s1, String... strings) { super(append(new String[] { s1 }, strings)); }
                }
                static String mod(String in) { return in.repeat(2); }
                static <T> T[] append(T[] t1, T[] t2) {
                    return Stream.concat(Arrays.stream(t1), Arrays.stream(t2)).toArray(); 
                }
            }
            """;

    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT4);
    }

}
