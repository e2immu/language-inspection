package org.e2immu.analyzer.shallow.aapi;

import org.e2immu.annotation.*;
import org.e2immu.annotation.rare.IgnoreModifications;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.analysis.PropertyValueMap;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ShallowAnalyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShallowAnalyzer.class);

    private final Runtime runtime;
    private final AnnotationProvider annotationProvider;

    public ShallowAnalyzer(Runtime runtime, AnnotationProvider annotationProvider) {
        this.annotationProvider = annotationProvider;
        this.runtime = runtime;
    }

    public void analyze(TypeInfo typeInfo) {
        if (typeInfo.analysis().getOrDefault(PropertyImpl.SHALLOW_ANALYZER, ValueImpl.BoolImpl.FALSE).isTrue()) {
            return; // already done
        }
        List<AnnotationExpression> annotations = annotationProvider.annotations(typeInfo);
        Map<Property, Value> map = typeAnnotationsToMap(typeInfo, annotations);
        map.forEach(typeInfo.analysis()::set);
    }

    public void check(TypeInfo typeInfo) {
        Value.Immutable immutable = typeInfo.analysis().getOrDefault(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.MUTABLE);
        if (immutable.isAtLeastImmutableHC()) {
            ensureHierarchyAtLeast(typeInfo, PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.MUTABLE, immutable);
        }
        Value.Bool container = typeInfo.analysis().getOrDefault(PropertyImpl.CONTAINER_TYPE, ValueImpl.BoolImpl.FALSE);
        if (container.isTrue()) {
            ensureHierarchyAtLeast(typeInfo, PropertyImpl.CONTAINER_TYPE, ValueImpl.BoolImpl.FALSE, container);
        }
        Value.Independent independent = typeInfo.analysis().getOrDefault(PropertyImpl.INDEPENDENT_TYPE, ValueImpl.IndependentImpl.DEPENDENT);
        if (independent.isAtLeastIndependentHc()) {
            ensureHierarchyAtLeast(typeInfo, PropertyImpl.CONTAINER_TYPE, ValueImpl.BoolImpl.FALSE, ValueImpl.IndependentImpl.INDEPENDENT_HC);
        }
        if (immutable.isImmutable() && !independent.isIndependent()
            || immutable.isAtLeastImmutableHC() && !independent.isAtLeastIndependentHc()) {
            LOGGER.warn("Inconsistency between independent and immutable");
        }
    }

    private void ensureHierarchyAtLeast(TypeInfo typeInfo, Property property, Value defaultValue, Value standard) {
        if (typeInfo.parentClass() != null) {
            TypeInfo parentType = typeInfo.parentClass().typeInfo();
            Value parentValue = parentType.analysis().getOrDefault(property, defaultValue);
            if (parentValue.compareTo(standard) < 0) {
                LOGGER.warn("Parent has lower value than child");
            }
            ensureHierarchyAtLeast(parentType, property, defaultValue, standard);
        }
        for (ParameterizedType interfaceImplemented : typeInfo.interfacesImplemented()) {
            TypeInfo interfaceType = interfaceImplemented.bestTypeInfo();
            Value interfaceValue = interfaceType.analysis().getOrDefault(property, defaultValue);
            if (interfaceValue.compareTo(standard) < 0) {
                LOGGER.warn("Implemented interface has lower value than type itself");
            }
            ensureHierarchyAtLeast(interfaceType, property, defaultValue, standard);
        }
    }

    private Map<Property, Value> typeAnnotationsToMap(TypeInfo typeInfo, List<AnnotationExpression> annotations) {
        int immutableLevel = 0;
        int independentLevel = -1;
        boolean isContainer = false;
        for (AnnotationExpression ae : annotations) {
            boolean isAbsent = ae.extractBoolean("absent");
            if (!isAbsent) {
                String fqn = ae.typeInfo().fullyQualifiedName();
                if (Immutable.class.getCanonicalName().equals(fqn)) {
                    boolean hc = ae.extractBoolean("hc");
                    immutableLevel = hc ? ValueImpl.ImmutableImpl.IMMUTABLE_HC.value()
                            : ValueImpl.ImmutableImpl.IMMUTABLE.value();
                } else if (ImmutableContainer.class.getCanonicalName().equals(fqn)) {
                    boolean hc = ae.extractBoolean("hc");
                    immutableLevel = hc ? ValueImpl.ImmutableImpl.IMMUTABLE_HC.value()
                            : ValueImpl.ImmutableImpl.IMMUTABLE.value();
                    isContainer = true;
                } else if (FinalFields.class.getCanonicalName().equals(fqn)) {
                    immutableLevel = ValueImpl.ImmutableImpl.FINAL_FIELDS.value();
                } else if (Container.class.getCanonicalName().equals(fqn)) {
                    isContainer = true;
                } else if (Independent.class.getCanonicalName().equals(fqn)) {
                    boolean hc = ae.extractBoolean("hc");
                    independentLevel = hc ? ValueImpl.IndependentImpl.INDEPENDENT_HC.value()
                            : ValueImpl.IndependentImpl.INDEPENDENT.value();
                }
            }
        }
        Value container = ValueImpl.BoolImpl.from(isContainer);
        Value.Immutable immutable = ValueImpl.ImmutableImpl.from(immutableLevel);
        Value independent;
        if (independentLevel == -1) {
            independent = simpleComputeIndependent(typeInfo, isContainer, immutable);
        } else {
            independent = ValueImpl.IndependentImpl.from(independentLevel);
        }
        return Map.of(PropertyImpl.IMMUTABLE_TYPE, immutable,
                PropertyImpl.INDEPENDENT_TYPE, independent,
                PropertyImpl.CONTAINER_TYPE, container);
    }

    private Value simpleComputeIndependent(TypeInfo typeInfo, boolean isContainer, Value.Immutable immutable) {
        if (immutable.isImmutable()) return ValueImpl.IndependentImpl.INDEPENDENT;
        if (immutable.isAtLeastImmutableHC()) return ValueImpl.IndependentImpl.INDEPENDENT_HC;
        Stream<MethodInfo> stream = Stream.concat(typeInfo.methodStream(), typeInfo.constructors().stream())
                .filter(MethodInfo::isPubliclyAccessible);

        boolean allMethodsOnlyPrimitives = stream.allMatch(m ->
                (m.isConstructor() || m.isVoid() || m.returnType().isPrimitiveStringClass())
                && m.parameters().stream().allMatch(p -> p.parameterizedType().isPrimitiveStringClass()));
        if (allMethodsOnlyPrimitives) {

        }
        return ValueImpl.IndependentImpl.DEPENDENT;
    }

}
