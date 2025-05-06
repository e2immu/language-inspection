package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestMethodCall8 extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.test;
            
            public class MethodCall_81 {
                @FunctionalInterface
                interface Customizer<T> {
                    T customize();
                    static <T> Customizer<T> withDefaults();
                }
                interface HttpSecurityBuilder<H extends HttpSecurityBuilder<H>> { }
                static abstract class AbstractHttpConfigurer<T extends AbstractHttpConfigurer<T,B>,
                                                             B extends HttpSecurityBuilder<B>> {
                    B disable();
                    T withObjectPostProcessor();
                }
                interface CorsConfigurer<H extends HttpSecurityBuilder<H>> { }
                static abstract class CsrfConfigurer<H extends HttpSecurityBuilder<H>>
                    extends AbstractHttpConfigurer<CsrfConfigurer<H>, H> { }
            
                interface HttpSecurity extends HttpSecurityBuilder<HttpSecurity> {
                    HttpSecurity cors(Customizer<CorsConfigurer<HttpSecurity>> customizer);
                    HttpSecurity csrf(Customizer<CsrfConfigurer<HttpSecurity>> csrfCustomizer) throws Exception;
                }
            
                public void method(HttpSecurity httpSecurity) {
                    httpSecurity.cors(Customizer.withDefaults())
                      .csrf(AbstractHttpConfigurer::disable);
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
                }
            
                static abstract class AbstractMockMvcBuilder<B extends AbstractMockMvcBuilder<B>>
                    implements ConfigurableMockMvcBuilder<B> {
                    @Override
                    <T extends B> T apply(MockMvcConfigurer configurer);
                    protected <T extends B> T self();
                    @Override
                    MockMvc build();
                }
                static abstract class DefaultMockMvcBuilder extends AbstractMockMvcBuilder<DefaultMockMvcBuilder> {
            
                }
                static class MockMvcBuilders {
                    static DefaultMockMvcBuilder webAppContextSetup(WebApplicationContext context) {
                        return null;
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
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 0);
        Statement statement = methodInfo.methodBody().lastStatement();
        if (statement.expression() instanceof MethodCall mc) {
            // FIXME this should be T extends DefaultMockMvcBuilder
            assertEquals("""
                    Type param T extends B extends org.e2immu.test.MethodCall_82.AbstractMockMvcBuilder<B>\
                    """, mc.object().parameterizedType().toString());
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
            import org.assertj.core.api.AbstractCollectionAssert;import org.assertj.core.api.ObjectAssert;import java.util.Collection;import java.util.Set;
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
    }

}
