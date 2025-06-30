package org.e2immu.language.inspection.integration.java.other;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.e2immu.language.cst.api.element.ModuleInfo;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestModuleInfo {
    @BeforeAll
    public static void beforeAll() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
    }

    @Test
    public void test() throws IOException {
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSources(InputConfigurationImpl.MAVEN_MAIN)
                .addRestrictSourceToPackages(".")
                .addClassPath(InputConfigurationImpl.GRADLE_DEFAULT)
                .build();
        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(inputConfiguration);
        ParseResult parseResult = javaInspector.parse(JavaInspectorImpl.FAIL_FAST).parseResult();
        assertEquals(1, parseResult.sourceSetsByName().size());
        SourceSet set = parseResult.sourceSetsByName().values().stream().findFirst().orElseThrow();
        ModuleInfo moduleInfo = set.moduleInfo();
        assertEquals("org.e2immu.language.inspection.integration", moduleInfo.name());
        assertEquals("RequiresImpl[name=org.e2immu.util.external.support, isStatic=false, isTransitive=false]",
                moduleInfo.requires().getFirst().toString());
        assertEquals(14, moduleInfo.requires().size());
    }
}
