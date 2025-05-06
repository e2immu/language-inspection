package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertSame;

public class TestEnum extends CommonTest {


    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            public class X {
                enum State { START, BUSY, END }

                int method(State state) {
                   return switch(state) {
                       case START -> 3;
                       case END -> 4;
                       default -> 0;
                   };
                }

                int method2(State state) {
                   switch(state) {
                       case START:
                           System.out.println("start");
                           break;
                       case END:
                           System.out.println("end");
                           break;
                   }
                   return -1;
                }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);

    }
}
