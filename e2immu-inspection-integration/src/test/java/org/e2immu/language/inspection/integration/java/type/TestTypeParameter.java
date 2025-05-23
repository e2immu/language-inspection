package org.e2immu.language.inspection.integration.java.type;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.TypeParameter;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTypeParameter extends CommonTest {

    @Language("java")
    public static final String INPUT5 = """
            package a.b;
            import org.e2immu.annotation.Container;
            import org.e2immu.annotation.Independent;
            class X {
              class Class$<@Independent @Container T> {
            
              }
            }
            """;

    @Test
    public void test5() {
        TypeInfo typeInfo = javaInspector.parse(INPUT5);
        TypeInfo clazz = typeInfo.findSubType("Class$");
        TypeParameter tp = clazz.typeParameters().getFirst();
        assertEquals(2, tp.annotations().size());
        assertEquals("""
                package a.b;
                import org.e2immu.annotation.Container;
                import org.e2immu.annotation.Independent;
                class X { class Class$<@Independent @Container T> { } }
                """, javaInspector.print2(typeInfo));
    }

}
