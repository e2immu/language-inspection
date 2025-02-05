package org.e2immu.language.inspection.integration.java.type;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestIsAssignableFrom extends CommonTest {

    @Test
    public void test() {
        TypeInfo closeable = javaInspector.compiledTypesManager().getOrLoad(Closeable.class);
        assertEquals("java.io.Closeable", closeable.fullyQualifiedName());
        TypeInfo iterable = javaInspector.compiledTypesManager().get(Iterable.class);
        assertEquals("java.lang.Iterable", iterable.fullyQualifiedName());
        Runtime runtime = javaInspector.runtime();
        ParameterizedType closeablePt = runtime.newParameterizedType(closeable, 0);
        ParameterizedType iterableCloseable = runtime.newParameterizedType(iterable, List.of(closeablePt));
        assertEquals("Iterable<java.io.Closeable>", iterableCloseable.fullyQualifiedName());
        ParameterizedType closeableArray = runtime.newParameterizedType(closeable, 1);
        assertEquals("java.io.Closeable[]", closeableArray.fullyQualifiedName());

        // there is no special code in IsAssignableFrom to make this work!
        // there is, however, special code in HiddenContentSelector to deal with the 'transfer'
        // from array base to type parameter
        assertFalse(iterableCloseable.isAssignableFrom(runtime, closeableArray));
    }
}
