package org.e2immu.language.inspection.integration.java.other;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.parsers.java.ParseException;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class TestCompilationProblem {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TestCompilationProblem.class);

    protected JavaInspector javaInspector;

    @BeforeAll
    public static void beforeAll() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
    }

    @Test
    public void test() throws IOException {
        javaInspector = new JavaInspectorImpl();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSources("src/test/resources/compilationError")
                .addRestrictSourceToPackages("a.")
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .addClassPath(JavaInspectorImpl.E2IMMU_SUPPORT)
                .build();
        javaInspector.initialize(inputConfiguration);
        try {
            javaInspector.parse(new JavaInspectorImpl.ParseOptionsBuilder().setFailFast(true).setDetailedSources(true).build());
        } catch (Summary.FailFastException ff) {
            Summary.ParseException e = (Summary.ParseException) ff.getCause();
            LOGGER.error("Parse exception", e.getCause() == null ? e : e.getCause());
            assertTrue(e.uri().toString().endsWith("compilationError/a/Faulty.java"));
            assertTrue(e.getMessage().contains("\nEncountered an error at (or somewhere around) input:4:33"));
            assertInstanceOf(ParseException.class, e.throwable());
        } catch (Exception e) {
            fail("This exception should not be raised: " + e.getClass());
        }
    }

    @Test
    public void test2FailFastFalse() throws IOException {
        javaInspector = new JavaInspectorImpl();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSources("src/test/resources/compilationError")
                .addRestrictSourceToPackages("a.")
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .addClassPath(JavaInspectorImpl.E2IMMU_SUPPORT)
                .build();
        javaInspector.initialize(inputConfiguration);
        try {
            Summary summary = javaInspector.parse(new JavaInspectorImpl.ParseOptionsBuilder().setFailFast(false).setDetailedSources(true).build());
            assertEquals(1, summary.parseExceptions().size());
            Summary.ParseException e = summary.parseExceptions().getFirst();
            assertTrue(e.uri().toString().endsWith("compilationError/a/Faulty.java"));
            assertTrue(e.getMessage().contains("\nEncountered an error at (or somewhere around) input:4:33"));
            assertInstanceOf(ParseException.class, e.throwable());
        } catch (Exception e) {
            fail("This exception should not be raised: " + e.getClass());
        }
    }
}
