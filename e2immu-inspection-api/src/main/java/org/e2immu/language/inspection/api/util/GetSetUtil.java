package org.e2immu.language.inspection.api.util;

import org.e2immu.annotation.Fluent;
import org.e2immu.annotation.method.GetSet;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.util.internal.util.GetSetHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
this class sits a little out of place in the inspection-api package, but it is currently the only place
that has access to runtime, and can be shared between java-bytecode and java-parser.

The alternative is to put this in Factory/FactoryImpl.
 */

public class GetSetUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetSetUtil.class);
    private final Runtime runtime;

    public GetSetUtil(Runtime runtime) {
        this.runtime = runtime;
    }

    public void createSyntheticFieldsCorrespondingToGetSetAnnotation(TypeInfo typeInfo) {
        TypeInfo.Builder builder = typeInfo.builder();
        builder.methods().stream().filter(MethodInfo::isAbstract).forEach(mi -> {
            AnnotationExpression getSet = mi.annotations().stream()
                    .filter(ae -> ae.typeInfo().fullyQualifiedName().equals(GetSet.class.getCanonicalName()))
                    .findFirst().orElse(null);
            if (getSet != null && !mi.isFactoryMethod() && !getSet.extractBoolean("equivalent")) {
                String fieldName;
                String proposed = getSet.extractString("value", "");
                if (!proposed.isBlank()) {
                    fieldName = proposed.trim();
                } else {
                    fieldName = GetSetHelper.fieldName(mi.name());
                }
                FieldInfo fieldInfo = builder.fields().stream().filter(f -> fieldName.equals(f.name())).findFirst().orElse(null);
                FieldInfo getSetField;
                boolean setter = mi.isVoid() || isComputeFluent(mi);
                int parameterIndexOfIndex = parameterIndexOfIndex(mi, setter);
                if (fieldInfo == null) {
                    LOGGER.debug("Create synthetic field for {}, named {}", mi, fieldName);
                    ParameterizedType type = extractFieldType(mi, setter, parameterIndexOfIndex);
                    FieldInfo syntheticField = runtime.newFieldInfo(fieldName, false, type, typeInfo);
                    syntheticField.builder().setSynthetic(true)
                            .addFieldModifier(runtime.fieldModifierPrivate())
                            .computeAccess()
                            .commit();
                    builder.addField(syntheticField);
                    getSetField = syntheticField;
                } else {
                    getSetField = fieldInfo;
                }
                runtime.setGetSetField(mi, getSetField, setter, parameterIndexOfIndex);
            }
        });
    }

    public static boolean isComputeFluent(MethodInfo mi) {
        String fluentFqn = Fluent.class.getCanonicalName();
        if (mi.annotations().stream().anyMatch(ae -> fluentFqn.equals(ae.typeInfo().fullyQualifiedName()))) {
            return true;
        }
        return !mi.methodBody().isEmpty()
               && mi.methodBody().lastStatement() instanceof ReturnStatement rs
               && rs.expression() instanceof VariableExpression ve && ve.variable() instanceof This;
    }

    public static int parameterIndexOfIndex(MethodInfo mi, boolean setter) {
        if (setter) {
            if (2 == mi.parameters().size()) {
                if (mi.parameters().get(0).parameterizedType().isInt()) return 0;
                if (mi.parameters().get(1).parameterizedType().isInt()) return 1;
            }
            return -1;
        }
        // getter
        return mi.parameters().size() == 1 && mi.parameters().get(0).parameterizedType().isInt() ? 0 : -1;
    }

    private static ParameterizedType extractFieldType(MethodInfo mi, boolean setter, int parameterIndexOfIndex) {
        if (mi.parameters().isEmpty()) {
            // T getT()
            assert !setter;
            return mi.returnType();
        }
        if (mi.parameters().size() == 1) {
            if (setter) {
                // void setT(T t)
                return mi.parameters().get(0).parameterizedType();
            }
            if (mi.parameters().get(0).parameterizedType().isInt()) {
                // T getT(int i)   INDEXED
                int a = mi.returnType().arrays();
                return mi.returnType().copyWithArrays(a + 1);
            }
            // Builder newBuilder(URI uri)
            throw new UnsupportedOperationException();
        }
        if (mi.parameters().size() == 2 && mi.parameters().get(parameterIndexOfIndex).parameterizedType().isInt()) {
            // void setObject(int i, Object o)  INDEXED
            assert setter;
            ParameterizedType p1 = mi.parameters().get(1 - parameterIndexOfIndex).parameterizedType();
            int a = p1.arrays();
            return mi.returnType().copyWithArrays(a + 1);
        }
        throw new UnsupportedOperationException();
    }
}
