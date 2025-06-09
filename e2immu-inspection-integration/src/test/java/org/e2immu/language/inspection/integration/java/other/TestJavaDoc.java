package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestJavaDoc extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                /**
                 * 
                 * @param in 
                 * @param len length of in
                 * @return 
                 * @throws IndexOutOfBoundsException
                 */
                public String method(String in, int len) {
                    if(len < 0 || len >= in.length()) throw new IndexOutOfBoundsException();
                    return in.substring(len);
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        @Language("java")
        String expected = """
                package a.b;
                class X {
                    /**
                    *
                    * @param in
                    * @param len length of in
                    * @return
                    * @throws IndexOutOfBoundsException
                    */
                
                    public String method(String in, int len) {
                        if(len < 0 ||len >= in.length()) { throw new IndexOutOfBoundsException(); }
                        return in.substring(len);
                    }
                }
                """;
        assertEquals(expected, javaInspector.print2(typeInfo));
    }
}
