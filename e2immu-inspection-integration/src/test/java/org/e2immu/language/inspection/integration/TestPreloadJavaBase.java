package org.e2immu.language.inspection.integration;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.TypeParameter;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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

        TypeInfo consumer = javaInspector.compiledTypesManager().get(Consumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.hasBeenInspected());

        TypeInfo iterable = javaInspector.compiledTypesManager().get(Iterable.class);
        assertNotNull(iterable);
        assertTrue(iterable.hasBeenInspected());
        MethodInfo iterator = iterable.singleAbstractMethod();
        assertEquals("java.lang.Iterable.iterator()", iterator.fullyQualifiedName());
        MethodInfo forEach = iterable.findUniqueMethod("forEach", 1);
        ParameterInfo forEach0 = forEach.parameters().get(0);
        assertSame(consumer, forEach0.parameterizedType().typeInfo());

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

        TypeInfo map = javaInspector.compiledTypesManager().get(Map.class);
        assertNotNull(map);
        TypeInfo entry = map.findSubType("Entry");
        assertTrue(entry.hasBeenInspected());
        assertFalse(entry.haveOnDemandInspection());

        TypeInfo string = javaInspector.compiledTypesManager().get(String.class);
        assertTrue(string.isSealedOrFinal());
    }

    @Test
    public void testPreloadJavaUtilStream() throws IOException {
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addClassPath(InputConfigurationImpl.DEFAULT_CLASSPATH)
                .build();
        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(inputConfiguration);
        javaInspector.preload("java.util.stream");
        javaInspector.loadByteCodeQueue();

        TypeInfo spinedBuffer = javaInspector.compiledTypesManager().get("java.util.stream.SpinedBuffer");
        assertNotNull(spinedBuffer);
        assertFalse(spinedBuffer.isPublic());
        TypeInfo ofPrimitive = spinedBuffer.findSubType("OfPrimitive");
        TypeParameter ofPrimitive0 = ofPrimitive.typeParameters().get(0);
        TypeInfo baseSpliterator = ofPrimitive.findSubType("BaseSpliterator");
        TypeParameter tp0 = baseSpliterator.typeParameters().get(0);
        assertEquals("T_SPLITR=TP#0 in BaseSpliterator", tp0.toString());
        assertEquals("Type java.util.Spliterator.OfPrimitive<E,T_CONS,T_SPLITR extends java.util.Spliterator.OfPrimitive<E,T_CONS,T_SPLITR>>",
                tp0.typeBounds().get(0).toString());
        // check that the E in the type bound is indeed the E of OfPrimitive
        assertEquals(ofPrimitive0, tp0.typeBounds().get(0).parameters().get(0).typeParameter());
    }

    @Test
    public void testPreloadJavaNet() throws IOException {
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addClassPath(InputConfigurationImpl.DEFAULT_CLASSPATH)
                .build();
        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(inputConfiguration);
        javaInspector.preload("java.net.http");
        javaInspector.loadByteCodeQueue();
        TypeInfo bodyHandler = javaInspector.compiledTypesManager().get("java.net.http.HttpResponse.BodyHandler");
        assertNotNull(bodyHandler);
    }
}