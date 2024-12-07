package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestField extends CommonTest {


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.List;
            class C {
              List<String> stringList;
              List<Integer> intList1 = List.of(), intList2, intList3 = null;
              int i, iArray[], j;
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2);
        assertEquals("C", typeInfo.simpleName());

        FieldInfo stringList = typeInfo.fields().get(0);
        assertEquals("Type java.util.List<String>", stringList.type().toString());
        FieldInfo intList1 = typeInfo.fields().get(1);
        assertEquals("Type java.util.List<Integer>", intList1.type().toString());
        assertEquals("List.of()", intList1.initializer().toString());
        FieldInfo intList2 = typeInfo.fields().get(2);
        assertEquals("Type java.util.List<Integer>", intList2.type().toString());
        assertTrue(intList2.initializer().isEmpty());
        FieldInfo intList3 = typeInfo.fields().get(3);
        assertEquals("Type java.util.List<Integer>", intList2.type().toString());
        assertEquals("null", intList3.initializer().toString());

        FieldInfo iArray = typeInfo.getFieldByName("iArray", true);
        assertEquals("Type int[]", iArray.type().toString());
        FieldInfo j = typeInfo.getFieldByName("j", true);
        assertEquals("Type int", j.type().toString());
    }
}
