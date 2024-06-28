package org.e2immu.analyzer.shallow.aapi;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.SourceTypes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestAnnotatedApiParser {
    @BeforeAll
    public static void beforeAll() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
    }

    @Test
    public void test() throws IOException {
        AnnotatedApiParser annotatedApiParser = new AnnotatedApiParser();
        annotatedApiParser.initialize(
                List.of("src/test/java/org/e2immu/analyzer/shallow/aapi"),
                List.of("example."));
        SourceTypes sourceTypes = annotatedApiParser.sourceTypes();
        List<TypeInfo> types = new ArrayList<>();
        sourceTypes.visit(new String[]{}, (parts, list) -> {
            types.addAll(list);
        });
        assertEquals(2, types.size());
        TypeInfo t1 = types.get(0);
        assertEquals("example.jdk.JavaLang", t1.fullyQualifiedName());
        String uri = t1.compilationUnitOrEnclosingType().getLeft().uri().toString();
        assertTrue(uri.endsWith("example/jdk/JavaLang.java"));
    }
}
