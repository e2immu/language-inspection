package org.e2immu.language.inspection.integration;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.*;

public class TestPreloadJavaBase {

    @Test
    public void testPreload() throws IOException {
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addClassPath(InputConfigurationImpl.DEFAULT_CLASSPATH)
                .build();
        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(inputConfiguration);
        javaInspector.loadByteCodeQueue();

        // NOTE: this may be very dependent on the current JDK and pre-loading settings.
        TypeInfo biConsumer = javaInspector.compiledTypesManager().get(BiConsumer.class);
        assertNotNull(biConsumer);
        assertTrue(biConsumer.hasBeenInspected());

        // interestingly, java.util.List has been referred to, but it has not been loaded
        // because it has not yet appeared in a type hierarchy (but it has appeared as a field type
        // in some private field of java.lang.Throwable)
        TypeInfo list = javaInspector.compiledTypesManager().get(List.class);
        assertNull(list);
        TypeInfo list2 = javaInspector.compiledTypesManager().getOrLoad(List.class);
        assertNotNull(list2);
        assertTrue(list2.hasBeenInspected());
    }
}
