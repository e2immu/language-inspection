package org.e2immu.language.inspection.integration.java.invalidate;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.e2immu.language.inspection.api.integration.JavaInspector.InvalidationState.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestInvalidate extends CommonTest2 {

    private static final String PROCESSOR_FQN = "a.b.util.Processor";

    @Language("java")
    String PROCESSOR = """
            package a.b.util;
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;
            public class Processor {
                private static final Logger LOGGER = LoggerFactory.getLogger(Processor.class);
            
                public Processor() {
                }
            
                public record ProcessResult(String someResult) {
                }
            }
            """;

    @Language("java")
    String ISOURCE = """
            package a.b;
            import a.b.util.Processor;
            public interface ISource { Processor.ProcessResult processResult(); }
            """;

    @Language("java")
    String SOURCE = """
            package a.b;
            import a.b.util.Processor;
            import java.util.Set;
            public record Source(String name, String src, Set<String> tags, Processor.ProcessResult processResult) implements ISource {
            }
            """;


    @Test
    public void test1() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.b.ISource", ISOURCE, "a.b.Source", SOURCE,
                PROCESSOR_FQN, PROCESSOR);
        ParseResult pr1 = init(sourcesByFqn);
        assertEquals(3, pr1.primaryTypes().size());
        Map<String, String> sourcesByURIString = sourcesByURIString(sourcesByFqn);

        // all unchanged
        JavaInspector.ParseOptions po2 = new JavaInspectorImpl.ParseOptionsBuilder()
                .setInvalidated(t -> UNCHANGED)
                .build();
        ParseResult pr2 = javaInspector.parse(sourcesByURIString, po2).parseResult();
        assertEquals(3, pr2.primaryTypes().size());
        for (TypeInfo pt1 : pr1.primaryTypes()) {
            TypeInfo pt2 = pr2.findType(pt1.fullyQualifiedName());
            assertSame(pt1, pt2);
        }
    }

    @Test
    public void test2() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.b.ISource", ISOURCE, "a.b.Source", SOURCE,
                PROCESSOR_FQN, PROCESSOR);
        ParseResult pr1 = init(sourcesByFqn);
        assertEquals(3, pr1.primaryTypes().size());
        Map<String, String> sourcesByURIString = sourcesByURIString(sourcesByFqn);

        // all changed
        JavaInspector.ParseOptions po2 = new JavaInspectorImpl.ParseOptionsBuilder()
                .setInvalidated(t -> INVALID)
                .build();
        ParseResult pr2 = javaInspector.parse(sourcesByURIString, po2).parseResult();
        assertEquals(3, pr2.primaryTypes().size());
        for (TypeInfo pt1 : pr1.primaryTypes()) {
            TypeInfo pt2 = pr2.findType(pt1.fullyQualifiedName());
            assertNotSame(pt1, pt2);
            assertEquals(pt1.fullyQualifiedName(), pt2.fullyQualifiedName());
            assertNotSame(pt1.compilationUnit(), pt2.compilationUnit());
            assertSame(pt1.compilationUnit().sourceSet(), pt2.compilationUnit().sourceSet());
        }
    }

    @Test
    public void test3() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.b.ISource", ISOURCE, "a.b.Source", SOURCE,
                PROCESSOR_FQN, PROCESSOR);
        ParseResult pr1 = init(sourcesByFqn);
        assertEquals(3, pr1.primaryTypes().size());
        Map<String, String> sourcesByURIString = sourcesByURIString(sourcesByFqn);

        // unchanged: Processor
        JavaInspector.ParseOptions po2 = new JavaInspectorImpl.ParseOptionsBuilder()
                .setInvalidated(t -> PROCESSOR_FQN.equals(t.fullyQualifiedName()) ? UNCHANGED : INVALID)
                .build();
        ParseResult pr2 = javaInspector.parse(sourcesByURIString, po2).parseResult();
        assertEquals(3, pr2.primaryTypes().size());
        for (TypeInfo pt1 : pr1.primaryTypes()) {
            TypeInfo pt2 = pr2.findType(pt1.fullyQualifiedName());
            if (PROCESSOR_FQN.equals(pt1.fullyQualifiedName())) {
                assertSame(pt1, pt2);
            } else {
                assertNotSame(pt1, pt2);
                assertEquals(pt1.fullyQualifiedName(), pt2.fullyQualifiedName());
                assertNotSame(pt1.compilationUnit(), pt2.compilationUnit());
                assertSame(pt1.compilationUnit().sourceSet(), pt2.compilationUnit().sourceSet());
            }
        }
    }


    @Test
    public void test4() throws IOException {
        Map<String, String> sourcesByFqn = Map.of("a.b.ISource", ISOURCE, "a.b.Source", SOURCE,
                PROCESSOR_FQN, PROCESSOR);
        ParseResult pr1 = init(sourcesByFqn);
        assertEquals(3, pr1.primaryTypes().size());
        Map<String, String> sourcesByURIString = sourcesByURIString(sourcesByFqn);

        // unchanged: Processor
        JavaInspector.ParseOptions po2 = new JavaInspectorImpl.ParseOptionsBuilder()
                .setInvalidated(t -> switch (t.simpleName()) {
                    case "Processor" -> UNCHANGED;
                    case "ISource" -> INVALID;
                    case "Source" -> REWIRE;
                    default -> throw new UnsupportedOperationException();
                })
                .build();
        ParseResult pr2 = javaInspector.parse(sourcesByURIString, po2).parseResult();
        assertEquals(3, pr2.primaryTypes().size());
        for (TypeInfo pt1 : pr1.primaryTypes()) {
            TypeInfo pt2 = pr2.findType(pt1.fullyQualifiedName());
            if (PROCESSOR_FQN.equals(pt1.fullyQualifiedName())) {
                assertSame(pt1, pt2);
            } else {
                assertNotSame(pt1, pt2);
                assertEquals(pt1.fullyQualifiedName(), pt2.fullyQualifiedName());
                if("ISource".equals(pt1.simpleName())) {
                    assertNotSame(pt1.compilationUnit(), pt2.compilationUnit());
                } else {
                    assertSame(pt1.compilationUnit(), pt2.compilationUnit());
                }
                assertSame(pt1.compilationUnit().sourceSet(), pt2.compilationUnit().sourceSet());
            }
        }
    }
}
