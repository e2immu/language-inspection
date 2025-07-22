package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.e2immu.language.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TestJavaDoc2 extends CommonTest2 {

    @Language("java")
    String aA = """
            package a;
            public class A {
                // empty
            }
            """;

    @Language("java")
    String bB = """
            package b;
            import a.A;
            /**
             * See {@link a.A}
             */
            public class B  {
                // empty
            }
            """;


    // do not touch the spacing here!
    @Language("java")
    String bB2 = """
            package b;
            import a.A;
                /**
                 * See {@link  a.A}
                 */
            public class B2  {
                // empty
            }
            """;

    @Test
    public void test() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.A", aA, "b.B", bB, "b.B2", bB2);
        ParseResult pr1 = init(sourcesByFqn);
        {
            TypeInfo B = pr1.findType("b.B");
            JavaDoc.Tag tag = B.javaDoc().tags().getFirst();
            assertEquals("a.A", tag.resolvedReference().toString());
            DetailedSources detailedSources = tag.source().detailedSources();
            assertNotNull(detailedSources);
            assertEquals("4-15:4-17", detailedSources.detail(tag.resolvedReference()).compact2());
            assertNull(detailedSources.associatedObject(tag.resolvedReference()));
            assertEquals("4-15:4-15", detailedSources.detail(((TypeInfo) tag.resolvedReference()).packageName()).compact2());
        }
        {
            TypeInfo B2 = pr1.findType("b.B2");
            JavaDoc.Tag tag = B2.javaDoc().tags().getFirst();
            assertEquals("a.A", tag.resolvedReference().toString());
            DetailedSources detailedSources = tag.source().detailedSources();
            assertNotNull(detailedSources);
            assertEquals("4-20:4-22", detailedSources.detail(tag.resolvedReference()).compact2());
            assertNull(detailedSources.associatedObject(tag.resolvedReference()));
            assertEquals("4-20:4-20", detailedSources.detail(((TypeInfo) tag.resolvedReference()).packageName()).compact2());
        }
    }
}
