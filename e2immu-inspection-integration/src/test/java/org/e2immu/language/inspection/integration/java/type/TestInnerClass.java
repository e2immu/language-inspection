package org.e2immu.language.inspection.integration.java.type;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

public class TestInnerClass extends CommonTest {
    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            class X {
                String x;
                class Y {
                    void method() {
                        System.out.println(x);
                    }
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
    }

    @Language("java")
    public static final String INPUT2 = """
            package a.b;
            class X {
                String[] x;
                class Y {
                    void method() {
                        System.out.println(x != null && x.length>0 ? x[0]: "?");
                    }
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
    }

}
