package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

public class TestFieldAccess extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.analyser.resolver.testexample;


            import java.util.HashMap;
            import java.util.HashSet;
            import java.util.Map;
            import java.util.Set;

            public class FieldAccess_0 {

                interface Analysis {
                }

                interface ParameterAnalysis extends Analysis {

                }

                static abstract class AnalysisImpl implements Analysis {
                    Set<Integer> properties = new HashSet<>();
                }

                static abstract class AbstractAnalysisBuilder implements Analysis {
                    Map<Integer, String> properties = new HashMap<>();
                }

                static class ParameterAnalysisImpl extends AnalysisImpl implements ParameterAnalysis {

                    static class Builder extends AbstractAnalysisBuilder implements ParameterAnalysis {

                    }
                }

                static class Test1 {
                    ParameterAnalysisImpl parameterAnalysis = new ParameterAnalysisImpl();

                    public boolean method(int i) {
                        return parameterAnalysis.properties.contains(i);
                    }
                }

                static class Test2 {
                    ParameterAnalysisImpl.Builder parameterAnalysis2 = new ParameterAnalysisImpl.Builder();

                    public boolean method(int i) {
                        return parameterAnalysis2.properties.containsKey(i);
                    }
                }
            }
            """;

    @Test
    public void test1() {
        javaInspector.parse(INPUT1);
    }

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;

            import java.util.ArrayList;
            import java.util.List;
            import java.util.stream.Stream;

            import static org.e2immu.analyser.resolver.testexample.importhelper.RMultiLevel.Effective.E1;

            public class FieldAccess_1 {

                interface Analyser {}

                abstract static class AbstractAnalyser implements Analyser {
                    public final String k = "3";
                    protected final List<String> messages = new ArrayList<>();

                    public List<String> getMessages() {
                        return messages;
                    }
                }

                abstract static class ParameterAnalyser extends AbstractAnalyser {
                    public final String s = "3";

                    public Stream<String> streamMessages() {
                        return messages.stream();
                    }
                }

                public static class CPA extends ParameterAnalyser {
                    public final String t = "3";

                    public void method() {
                        messages.add("3: "+E1);
                    }
                }
            }
            """;

    @Test
    public void test2() {
        javaInspector.parse(INPUT2);
    }


    @Language("java")
    private static final String INPUT3 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.util.Map;
            import java.util.stream.Stream;
            
            public record FieldAccess_2(Container c) {
            
                interface VIC {
                    String current();
                }
            
                record Variables(Map<String, VIC> variables) {
                    Stream<Map.Entry<String, VIC>> stream() {
                        return variables.entrySet().stream();
                    }
                }
            
                record Container(Variables v) {
            
                }
            
                public void test() {
                    c.v.stream().map(Map.Entry::getValue).forEach(vic -> System.out.println(vic.current()));
                }
            
                public void test2() {
                    c.v.stream().map(java.util.Map.Entry::getValue).forEach(vic -> System.out.println(vic.current()));
                }
            
            }
            """;

    @Test
    public void test3() {
        javaInspector.parse(INPUT3);
    }
}
