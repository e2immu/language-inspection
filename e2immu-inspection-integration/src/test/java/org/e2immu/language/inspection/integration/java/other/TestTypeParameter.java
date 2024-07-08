package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestTypeParameter extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.analyser.resolver.testexample;

            import org.e2immu.annotation.Modified;
            import org.e2immu.annotation.NotModified;
            import org.e2immu.annotation.NotNull;
            import org.e2immu.annotation.eventual.Only;
            import org.e2immu.support.Freezable;

            import java.util.*;
            import java.util.function.BiConsumer;

            public class TypeParameter_0 <T> extends Freezable {

            private static class Node<S> {
                List<S> dependsOn;
                final S t;

                private Node(S t) {
                    this.t = t;
                }

                private Node(S t, List<S> dependsOn) {
                    this.t = t;
                    this.dependsOn = dependsOn;
                }
            }

                @NotModified(after = "frozen")
                private final Map<T, Node<T>> nodeMap = new HashMap<>();

                @NotModified
                public int size() {
                    return nodeMap.size();
                }

                @NotModified
                public boolean isEmpty() {
                    return nodeMap.isEmpty();
                }
                @NotNull
                @Modified
                @Only(before = "frozen")
                private Node<T> getOrCreate(@NotNull T t) {
                    ensureNotFrozen();
                    Objects.requireNonNull(t);
                    Node<T> node = nodeMap.get(t);
                    if (node == null) {
                        node = new Node<>(t);
                        nodeMap.put(t, node);
                    }
                    return node;
                }

                @NotModified(contract = true)
                public void visit(@NotNull BiConsumer<T, List<T>> consumer) {
                    nodeMap.values().forEach(n -> consumer.accept(n.t, n.dependsOn));
                }

                @Only(before = "frozen")
                @Modified
                public void addNode(@NotNull @NotModified T t, @NotNull(content = true) Collection<T> dependsOn, boolean bidirectional) {
                    ensureNotFrozen();
                    Node<T> node = getOrCreate(t);
                    for (T d : dependsOn) {
                        if (node.dependsOn == null) node.dependsOn = new LinkedList<>();
                        node.dependsOn.add(d);
                        if (bidirectional) {
                            Node<T> n = getOrCreate(d);
                            if (n.dependsOn == null) n.dependsOn = new LinkedList<>();
                            n.dependsOn.add(t);
                        }
                    }
                }

            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
    }

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;

            import org.e2immu.annotation.Modified;
            import org.e2immu.annotation.NotModified;
            import org.e2immu.annotation.NotNull;
            import org.e2immu.annotation.eventual.Only;
            import org.e2immu.support.Freezable;

            import java.util.*;

            /*
            type parameter has a bit of a weird name, to facilitate debugging.
            T as a name is too common, and processing of byte code will also go through ParameterizedTypeFactory.
             */
            public class TypeParameter_1<TP0> extends Freezable {

                private static class Node<TP0> {
                    List<TP0> dependsOn;
                    final TP0 t;

                    private Node(TP0 t) {
                        this.t = t;
                    }

                    private Node(TP0 t, List<TP0> dependsOn) {
                        this.t = t;
                        this.dependsOn = dependsOn;
                    }
                }

                @NotModified(after = "frozen")
                private final Map<TP0, Node<TP0>> nodeMap = new HashMap<>();

                @NotNull
                @Modified
                @Only(before = "frozen")
                private Node<TP0> getOrCreate(@NotNull TP0 t) {
                    ensureNotFrozen();
                    Objects.requireNonNull(t);
                    Node<TP0> node = nodeMap.get(t);
                    if (node == null) {
                        node = new Node<>(t);
                        nodeMap.put(t, node);
                    }
                    return node;
                }

                @Only(before = "frozen")
                @Modified
                public void addNode(@NotNull @NotModified TP0 t, @NotNull(content = true) Collection<TP0> dependsOn, boolean bidirectional) {
                    for (TP0 d : dependsOn) {
                        if (bidirectional) {
                            Node<TP0> n = getOrCreate(d);
                            if (n.dependsOn == null) n.dependsOn = new LinkedList<>();
                            n.dependsOn.add(t);
                        }
                    }
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2);
    }

    @Language("java")
    private static final String INPUT3 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.lang.reflect.Array;

            public class TypeParameter_2 {

                static class WithId implements Cloneable {
                    int id;

                    @Override
                    public Object clone() {
                        try {
                            return super.clone();
                        } catch (CloneNotSupportedException ignore) {
                            throw new RuntimeException();
                        }
                    }
                }

                public static <T extends WithId> T[] method(T[] withIds) {
                    T[] result = (T[]) Array.newInstance(withIds.getClass().getComponentType(), withIds.length);
                    for (int i = 0;i < withIds.length;++i) {
                        if (withIds[i] != null) {
                            result[i] = (T) withIds[i].clone();
                            result[i].id = 4;
                        }
                    }
                    return result;
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = javaInspector.parse(INPUT3);
    }

    @Language("java")
    private static final String INPUT4 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.io.Serializable;

            public class TypeParameter_4 {
                static class QName implements Serializable {
                    String localPart;
                    String getLocalPart() {
                        return localPart;
                    }
                }
                static class JAXBElement<T> implements Serializable {
                    T value;
                    QName name;
                    public T getValue() {
                        return value;
                    }
                    public QName getName() { return name; }
                }

                public String method(Object object, String name) {
                    if (object instanceof JAXBElement && name.equalsIgnoreCase(((JAXBElement<?>) object).getName().getLocalPart())) {
                        return ((JAXBElement<?>) object).getValue().toString();
                    }
                    return "?";
                }
            }
            """;

    @Test
    public void test4() {
        TypeInfo typeInfo = javaInspector.parse(INPUT4);
    }

    @Language("java")
    private static final String INPUT5 = """

            package org.e2immu.analyser.resolver.testexample;

            import java.util.HashMap;

            public class TypeParameter_5 {

                interface Base {
                    long getId();
                }

                static class MyHashMap<T> extends HashMap<Long, T> {
                    @Override
                    public T put(Long key, T value) {
                        assert key != null;
                        assert value != null;
                        return super.put(key, value);
                    }
                }

                static <T extends Base> MyHashMap<T> mapDataByID(T[] data) {
                    MyHashMap<T> result = new MyHashMap<>();
                    for (int i = 0; i < data.length; ++i) {
                        result.put(data[i].getId(), data[i]);
                    }
                    return result;
                }

                <T extends Base> void method1(T[] data) {
                    MyHashMap objById = mapDataByID(data);
                }
            }
            """;

    @Test
    public void test5() {
        TypeInfo typeInfo = javaInspector.parse(INPUT5);
        TypeInfo myHashMap = typeInfo.findSubType("MyHashMap");
        MethodInfo put = myHashMap.findUniqueMethod("put", 2);
        if (put.methodBody().statements().get(2) instanceof ReturnStatement rs && rs.expression() instanceof MethodCall mc) {
            assertEquals("java.util.HashMap.put(K,V)", mc.methodInfo().fullyQualifiedName());
            assertEquals("super.put(key,value)", mc.toString());
            assertTrue(mc.object() instanceof VariableExpression ve && ve.variable() instanceof This thisVar && thisVar.writeSuper());
        } else fail();
    }

}
