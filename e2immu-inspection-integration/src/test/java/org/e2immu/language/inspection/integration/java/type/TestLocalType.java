package org.e2immu.language.inspection.integration.java.type;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

public class TestLocalType extends CommonTest {

    @Language("java")
    public static final String INPUT2 = """
            package a.b;
            class X {
                interface A { void method(String s); }
                A make(String t) {
                    final class C implements A {
                        @Override
                        void method(String s) {
                            System.out.println(s+t);
                        }
                    }
                    return new C();
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
    }

}
