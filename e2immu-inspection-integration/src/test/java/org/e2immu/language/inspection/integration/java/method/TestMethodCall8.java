package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestMethodCall8 extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.test;
            
            public class MethodCall_81 {
                @FunctionalInterface
                interface Customizer<T> {
                    void customize(T t);
                    static <T> Customizer<T> withDefaults() { return null; }
                }
                interface HttpSecurityBuilder<H extends HttpSecurityBuilder<H>> { }
                static abstract class AbstractHttpConfigurer<T extends AbstractHttpConfigurer<T,B>,
                        B extends HttpSecurityBuilder<B>> {
                    B disable() { return null; }
                    T withObjectPostProcessor() { return null; }
                }
                interface CorsConfigurer<H extends HttpSecurityBuilder<H>> { }
                static abstract class CsrfConfigurer<H extends HttpSecurityBuilder<H>>
                        extends AbstractHttpConfigurer<CsrfConfigurer<H>, H> { }
        
                interface HttpSecurity extends HttpSecurityBuilder<HttpSecurity> {
                    HttpSecurity cors(Customizer<CorsConfigurer<HttpSecurity>> customizer);
                    HttpSecurity csrf(Customizer<CsrfConfigurer<HttpSecurity>> csrfCustomizer) throws Exception;
                }
        
                public void method(HttpSecurity httpSecurity) throws Exception {
                    httpSecurity.cors(Customizer.withDefaults())
                            .csrf(AbstractHttpConfigurer::disable);
                }
            }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        TypeInfo customizer = typeInfo.findSubType("Customizer");
        assertTrue(customizer.isFunctionalInterface());
    }

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.test;
            
            public abstract class MethodCall_82 {
                interface MockMvc {}
                interface WebApplicationContext {}
                WebApplicationContext context;
                interface MockMvcConfigurer { }
                abstract MockMvcConfigurer springSecurity();
            
                interface MockMvcBuilder {
                    MockMvc build();
                }
            
                interface ConfigurableMockMvcBuilder<B extends ConfigurableMockMvcBuilder<B>> extends MockMvcBuilder {
                    <T extends B> T apply(MockMvcConfigurer configurer);
                    <T extends B> T dispatchOptions(boolean dispatchOptions);
                }
            
                static abstract class AbstractMockMvcBuilder<B extends AbstractMockMvcBuilder<B>>
                    implements ConfigurableMockMvcBuilder<B> {
                    @Override
                    <T extends B> T apply(MockMvcConfigurer configurer);
                    protected <T extends B> T self();
                    @Override
                    MockMvc build();
                }
                static class DefaultMockMvcBuilder extends AbstractMockMvcBuilder<DefaultMockMvcBuilder> {
                    DefaultMockMvcBuilder(WebApplicationContext webAppContext) { }
                }
                static class MockMvcBuilders {
                    static DefaultMockMvcBuilder webAppContextSetup(WebApplicationContext context) {
                        return new DefaultMockMvcBuilder(context);
                    }
                }
                MockMvc method() {
                   return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2);
        TypeInfo ConfigurableMockMvcBuilder = typeInfo.findSubType("ConfigurableMockMvcBuilder");
        assertFalse(ConfigurableMockMvcBuilder.isFunctionalInterface());
        TypeInfo MockMvcBuilder = typeInfo.findSubType("MockMvcBuilder");
        assertTrue(MockMvcBuilder.isFunctionalInterface());

        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 0);
        Statement statement = methodInfo.methodBody().lastStatement();
        if (statement.expression() instanceof MethodCall mc) {
            assertEquals("Type param T extends B", mc.object().parameterizedType().toString());
        }
    }


    /*
    assertThat method:
        static <E> org.assertj.core.api.AbstractCollectionAssert<?,java.util.Collection<? extends E>,
                                                                 E,org.assertj.core.api.ObjectAssert<E>>
            assertThat(java.util.Collection<? extends E> actual);

        abstract class AbstractCollectionAssert <SELF extends org.assertj.core.api.AbstractCollectionAssert<SELF,ACTUAL,ELEMENT,ELEMENT_ASSERT>,
                                                ACTUAL extends java.util.Collection<? extends ELEMENT>,
                                                ELEMENT,
                                                ELEMENT_ASSERT extends org.assertj.core.api.AbstractAssert<ELEMENT_ASSERT,ELEMENT>>
            extends org.assertj.core.api.AbstractIterableAssert<SELF,ACTUAL,ELEMENT,ELEMENT_ASSERT> {

        abstract class AbstractIterableAssert <SELF extends org.assertj.core.api.AbstractIterableAssert<SELF,ACTUAL,ELEMENT,ELEMENT_ASSERT>,
                                               ACTUAL extends java.lang.Iterable<? extends ELEMENT>,
                                               ELEMENT,
                                               ELEMENT_ASSERT extends org.assertj.core.api.AbstractAssert<ELEMENT_ASSERT,ELEMENT>>
           extends org.assertj.core.api.AbstractAssert<SELF,ACTUAL>
           implements org.assertj.core.api.ObjectEnumerableAssert<SELF,ELEMENT> {

        interface ObjectEnumerableAssert<SELF extends ObjectEnumerableAssert<SELF, ELEMENT>, ELEMENT> extends
                EnumerableAssert<SELF, ELEMENT>
        interface EnumerableAssert<SELF extends EnumerableAssert<SELF,ELEMENT>,ELEMENT> { SELF hasSize(int); }


        The problem is that the result of assertThat is an ObjectAssert object, rather than an AbstractCollectionAssert.
        The overload of ObjectAssert should have a lower priority.
     */
    @Language("java")
    private static final String INPUT3 = """
            package org.e2immu.test;
            import org.assertj.core.api.AbstractCollectionAssert;
            import org.assertj.core.api.ObjectAssert;
            import java.util.Collection;
            import java.util.Set;
            import static org.assertj.core.api.Assertions.assertThat;
            
            public class MethodCall_82 {
                static class LanguageDTO { }
                static abstract class EventDTO {
                    abstract Set<LanguageDTO> getProposalLanguages();
                }
                void method1() {
                    EventDTO eventDTOResult = new EventDTO();
                    AbstractCollectionAssert<?,Collection<? extends org.e2immu.test.MethodCall_82.LanguageDTO>,org.e2immu.test.MethodCall_82.LanguageDTO,ObjectAssert<org.e2immu.test.MethodCall_82.LanguageDTO>> collectionLanguageDTOObjectAssertAbstractCollectionAssert 
                      = assertThat(eventDTOResult.getProposalLanguages());
                    collectionLanguageDTOObjectAssertAbstractCollectionAssert.hasSize(2);
                }
                 void method2() {
                    EventDTO eventDTOResult = new EventDTO();
                    assertThat(eventDTOResult.getProposalLanguages()).hasSize(2);
                }
            }
            """;

    @Test
    public void test3() {
        javaInspector.parse(INPUT3);
        TypeInfo aca = javaInspector.compiledTypesManager().getOrLoad("org.assertj.core.api.AbstractCollectionAssert");
        String print = javaInspector.print2(aca);
        @Language("java")
        String expected = """
                package org.assertj.core.api;
                import java.util.Collection;
                import org.assertj.core.annotations.Beta; 
                public abstract class AbstractCollectionAssert<
                    SELF extends AbstractCollectionAssert<SELF, ACTUAL, ELEMENT, ELEMENT_ASSERT>,
                    ACTUAL extends Collection<? extends ELEMENT>,
                    ELEMENT,
                    ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>> extends AbstractIterableAssert<
                    SELF,
                    ACTUAL,
                    ELEMENT,
                    ELEMENT_ASSERT> {
                    protected AbstractCollectionAssert(ACTUAL actual, Class<?> selfType) { }
                    @Beta public SELF isUnmodifiable() { }
                    private void assertIsUnmodifiable() { }
                    private void expectUnsupportedOperationException(Runnable runnable, String method) { }
                    private <E extends ELEMENT> Collection<E> emptyCollection() { }
                }
                """;
        assertEquals(expected, print);
    }

    @Language("java")
    private static final String INPUT4 = """
            package org.e2immu.test;
            
            public class MethodCall_84 {
                interface GenericContainer<SELF extends GenericContainer<SELF>> { }
                static class GreenMailContainer<SELF extends GreenMailContainer<SELF>> extends GenericContainer<SELF> {
                    GreenMailContainer() { }
                    SELF withAuthEnabled(boolean authEnabled) { return this; }
                    SELF withReuse(boolean reuse) { return this; }
                }
                void method0() {
                    GreenMailContainer<?> gmc = new GreenMailContainer<>().withAuthEnabled(true);
                }
                void method1() {
                    var gmc = new GreenMailContainer<>().withAuthEnabled(true);
                }
                void method2() {
                    GreenMailContainer<?>  gmc = new GreenMailContainer<>().withAuthEnabled(true).withReuse(true);
                }
                void method3() {
                    var gmc = new GreenMailContainer<>().withAuthEnabled(true).withReuse(true);
                }
            }
            """;

    @Test
    public void test4() {
        TypeInfo typeInfo = javaInspector.parse(INPUT4);
        {
            MethodInfo method = typeInfo.findUniqueMethod("method1", 0);
            LocalVariableCreation lvc = (LocalVariableCreation) method.methodBody().lastStatement();
            assertEquals("Type param SELF extends org.e2immu.test.MethodCall_84.GreenMailContainer<SELF>",
                    lvc.localVariable().parameterizedType().toString());
            assertEquals(lvc.localVariable().parameterizedType(), lvc.localVariable().assignmentExpression().parameterizedType());
        }
        {
            MethodInfo method = typeInfo.findUniqueMethod("method3", 0);
            LocalVariableCreation lvc = (LocalVariableCreation) method.methodBody().lastStatement();
            assertEquals("Type param SELF extends org.e2immu.test.MethodCall_84.GreenMailContainer<SELF>",
                    lvc.localVariable().parameterizedType().toString());
            assertEquals(lvc.localVariable().parameterizedType(), lvc.localVariable().assignmentExpression().parameterizedType());
        }
    }


    @Language("java")
    private static final String INPUT5 = """
            package org.e2immu.test;
            import java.util.concurrent.CompletableFuture;
            
            public class MethodCall_85 {
                interface User { String getEmail(); }
                interface SendMailService {
                   CompletableFuture<Object> sendMail(String to, String[] bcc, String subject, String content,
                      boolean isHtml);
                }
                SendMailService sendMailService;
                void method(User user, String content) {
                    sendMailService.sendMail(user.getEmail(), null, "activation key", content, false);
                }
            }
            """;

    @DisplayName("null to String[]")
    @Test
    public void test5() {
        javaInspector.parse(INPUT5);
    }


    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            import java.util.Set;
            import java.util.List;
            class X {
                record R(Set<String> set, List<Integer> list, int i) {}
                static class Builder {
                    Set<String> stringSet;
                    List<Integer> intList;
                    int j;
                    Builder setStringSet(Set<String> set) { stringSet = set; return this; }
                    Builder setIntList(List<Integer>list) { intList = list; return this; }
                    Builder setJ(int k) { j = k; return this; }
                    R build() { return new R(stringSet, intList, j); }
                }
                R method(Set<String> in) {
                    Builder b = new Builder().setJ(3).setIntList(List.of(0, 1)).setStringSet(in);
                    R r = b.build();
                    return r;
                }
            }
            """;
    @DisplayName("which version of list?")
    @Test
    public void test6() {
        TypeInfo X = javaInspector.parse(INPUT6);
        MethodInfo method = X.findUniqueMethod("method", 1);
        LocalVariableCreation lvc0 = (LocalVariableCreation) method.methodBody().statements().getFirst();
        MethodCall setStringSet = (MethodCall) lvc0.localVariable().assignmentExpression();
        MethodCall setIntList = (MethodCall) setStringSet.object();
        MethodCall listOf = (MethodCall) setIntList.parameterExpressions().getFirst();
        assertEquals("java.util.List.of(E,E)", listOf.methodInfo().fullyQualifiedName());
    }
}
