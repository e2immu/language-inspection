package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestOverridesOfRecordAccessors extends CommonTest2 {

    @Language("java")
    String ISOURCE = """
            package io.codelaser.jfocus.stdbase.viewer;
            import io.codelaser.jfocus.stdbase.viewer.util.Processor;
            public interface ISource { Processor.ProcessResult processResult(); }
            """;

    @Language("java")
    String SOURCE = """
            package io.codelaser.jfocus.stdbase.viewer;
            import io.codelaser.jfocus.stdbase.viewer.util.Processor;
            import java.util.Set;
            public record Source(String name, String src, Set<String> tags, Processor.ProcessResult processResult) implements ISource {
            }
            """;

    @Language("java")
    String PROCESSOR = """
            package io.codelaser.jfocus.stdbase.viewer.util;
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

    @Test
    public void test() throws IOException {
        ParseResult parseResult = init(Map.of("io.codelaser.jfocus.stdbase.viewer.ISource", ISOURCE,
                "io.codelaser.jfocus.stdbase.viewer.Source", SOURCE,
                "io.codelaser.jfocus.stdbase.viewer.util.Processor", PROCESSOR));
        assertEquals(3, parseResult.primaryTypes().size());
    }
}
