package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 2);
        JavaDoc javaDoc = methodInfo.javaDoc();
        assertNotNull(javaDoc);
        assertEquals(4, javaDoc.tags().size());
        JavaDoc.Tag t0 = javaDoc.tags().getFirst();
        assertEquals("a.b.X.method(String,int):0:in", t0.resolvedReference().toString());
        assertEquals("5-15:5-16", t0.sourceOfReference().compact2());
        JavaDoc.Tag t1 = javaDoc.tags().get(1);
        assertEquals("a.b.X.method(String,int):1:len", t1.resolvedReference().toString());
        assertEquals("6-15:6-17", t1.sourceOfReference().compact2());

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

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            class X {
                /**
                 * ll {@link X#method(String,int}
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
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2);

    }
}
