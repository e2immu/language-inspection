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

}
