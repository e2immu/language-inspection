package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 2);
        JavaDoc javaDoc = methodInfo.javaDoc();
        assertNotNull(javaDoc);
        assertEquals(5, javaDoc.tags().size());
        JavaDoc.Tag link = javaDoc.tags().getFirst();
        assertEquals("X#method(String,int", link.content());
        assertNull(link.resolvedReference());
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            class X {
                class A {
                    /**
                     * ll {@link A#method(String,int)}
                     * ll {@link X.A#method(String,int)}
                     * ll {@link a.b.X.A#method(String,int)}
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
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = javaInspector.parse(INPUT3, JavaInspectorImpl.DETAILED_SOURCES);
        TypeInfo A = typeInfo.findSubType("A", true);
        MethodInfo methodInfo = A.findUniqueMethod("method", 2);
        JavaDoc javaDoc = methodInfo.javaDoc();
        assertNotNull(javaDoc);
        assertEquals(7, javaDoc.tags().size());
        {
            JavaDoc.Tag link = javaDoc.tags().getFirst();
            assertEquals("A#method(String,int)", link.content());
            MethodInfo method = (MethodInfo) link.resolvedReference();
            assertEquals("a.b.X.A.method(String,int)", method.fullyQualifiedName());
            assertEquals("5-14:5-42", link.source().compact2());
            //noinspection ALL
            List<DetailedSources.Builder.TypeInfoSource> tis = (List<DetailedSources.Builder.TypeInfoSource>)
                    link.source().detailedSources().associatedObject(method.typeInfo());
            assertNull(tis);
        }
        {
            JavaDoc.Tag link = javaDoc.tags().get(1);
            assertEquals("X.A#method(String,int)", link.content());
            MethodInfo method = (MethodInfo) link.resolvedReference();
            assertEquals("a.b.X.A.method(String,int)", method.fullyQualifiedName());
            assertEquals("6-14:6-44", link.source().compact2());
            //noinspection ALL
            List<DetailedSources.Builder.TypeInfoSource> tis = (List<DetailedSources.Builder.TypeInfoSource>)
                    link.source().detailedSources().associatedObject(method.typeInfo());
            assertEquals(1, tis.size());
            assertEquals("6-14:6-14", tis.getFirst().source().compact2());
            assertNull(link.source().detailedSources().detail(method.typeInfo().packageName()));
        }
        {
            JavaDoc.Tag link = javaDoc.tags().get(2);
            assertEquals("a.b.X.A#method(String,int)", link.content());
            MethodInfo method = (MethodInfo) link.resolvedReference();
            assertEquals("a.b.X.A.method(String,int)", method.fullyQualifiedName());
            assertEquals("7-14:7-48", link.source().compact2());
            //noinspection ALL
            List<DetailedSources.Builder.TypeInfoSource> tis = (List<DetailedSources.Builder.TypeInfoSource>)
                    link.source().detailedSources().associatedObject(method.typeInfo());
            assertEquals(1, tis.size());
            assertEquals("7-14:7-18", tis.getFirst().source().compact2());
            assertEquals("7-14:7-16",
                    link.source().detailedSources().detail(method.typeInfo().packageName()).compact2());
        }
    }
}
