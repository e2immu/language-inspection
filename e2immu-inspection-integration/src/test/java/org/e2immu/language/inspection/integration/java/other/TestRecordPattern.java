package org.e2immu.language.inspection.integration.java.other;


import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestRecordPattern extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X1 {
                interface I { }
                record RI(String s) implements I { }
                void method(I i) {
                    if(i instanceof RI(String t)) {
                        System.out.println(t);
                    }
                }
            }
            """;

    @DisplayName("record pattern 1, basics")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);

    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            class X2 {
                interface I<T> { }
                record RI<T>(T t) implements I<T> { }
                void method(I<String> i) {
                    if(i instanceof RI<String>(String s)) {
                        System.out.println(s);
                    }
                }
            }
            """;

    @DisplayName("record pattern 2, type parameter")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);

    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            class X3 {
                interface I { }
                record RI(int j) implements I { }
                void method(I i) {
                    if(i instanceof RI(int k)) {
                        System.out.println(k);
                    }
                }
            }
            """;

    @DisplayName("record pattern 3, primitive type")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);

    }

    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            class X4 {
                record Point(int x, int y) {}
                enum Color { RED, GREEN, BLUE }
                record ColoredPoint(Point p, Color c) {}
                record Rectangle(ColoredPoint upperLeft, ColoredPoint lowerRight) {}
                static void printUpperLeftColoredPoint(Rectangle r) {
                    if (r instanceof Rectangle(ColoredPoint ul, ColoredPoint lr)) {
                         System.out.println(ul.c());
                    }
                }
                static void printColorOfUpperLeftPoint2(Rectangle r) {
                    if (r instanceof Rectangle(ColoredPoint(Point p, Color c),
                                               ColoredPoint lr)) {
                        System.out.println(c);
                    }
                }
            }
            """;

    @DisplayName("record pattern 4, nested")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT4);

    }

    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            class X5 {
                record Point(int x, int y) {}
                enum Color { RED, GREEN, BLUE }
                record ColoredPoint(Point p, Color c) {}
                record Rectangle(ColoredPoint upperLeft, ColoredPoint lowerRight) {}
                static void printXCoordOfUpperLeftPointWithPatterns(Rectangle r) {
                    if (r instanceof Rectangle(ColoredPoint(Point(var x, var y), var c), var lr)) {
                         System.out.println("Upper-left corner: " + x);
                    }
                }
                static void printXCoordOfUpperLeftPointWithPatterns2(Rectangle r) {
                    if (r instanceof Rectangle(ColoredPoint(Point(var x, var y), var c), _)) {
                         System.out.println("Upper-left corner: " + x);
                    }
                }
            }
            """;

    @DisplayName("record pattern 5, var, unnamed")
    @Test
    public void test5() {
        TypeInfo X = javaInspector.parse(INPUT5);

    }
}


