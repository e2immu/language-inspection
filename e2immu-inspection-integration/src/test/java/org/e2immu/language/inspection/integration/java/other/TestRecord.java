package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestRecord extends CommonTest {

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import org.e2immu.annotation.method.GetSet;
            import java.util.ArrayList;
            import java.util.HashSet;
            import java.util.Set;
            import java.util.List;
            class X {
                interface R {
                    @GetSet Set<Integer> set();
                    int i();
                }
                record RI(Set<Integer> set, int i, List<String> list) implements R {}
            
                void setAdd(R r) {
                    r.set().add(r.i());
                }
            
                void method() {
                    List<String> l = new ArrayList<>();
                    Set<Integer> s = new HashSet<>();
                    R r = new RI(s, 3, l);
                    setAdd(r); // at this point, s1 should have been modified, via???
                }
            }
            """;

    @DisplayName("combination test of record implementing an interface")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);

        // check that @GetSet works at the level R
        TypeInfo R = X.findSubType("R");
        MethodInfo Rset = R.findUniqueMethod("set", 0);
        FieldInfo RsetField = R.getFieldByName("set", true);
        assertSame(RsetField, Rset.getSetField().field());

        // check that @GetSet works at the level RI
        TypeInfo RI = X.findSubType("RI");
        MethodInfo RIset = RI.findUniqueMethod("set", 0);
        FieldInfo RIsetField = RI.getFieldByName("set", true);
        assertSame(RIsetField, RIset.getSetField().field());

        // also, verify the override
        assertTrue(RIset.overrides().contains(Rset));
    }
}
