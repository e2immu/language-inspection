package org.e2immu.language.inspection.api.util;

import org.e2immu.annotation.method.GetSet;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
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

    public static void createSyntheticFieldsCorrespondingToGetSetAnnotation(Runtime runtime, TypeInfo typeInfo) {
        TypeInfo.Builder builder = typeInfo.builder();;
        builder.methods().stream().filter(MethodInfo::isAbstract).forEach(mi -> {
            AnnotationExpression getSet = mi.annotations().stream()
                    .filter(ae -> ae.typeInfo().fullyQualifiedName().equals(GetSet.class.getCanonicalName()))
                    .findFirst().orElse(null);
            if (getSet != null) {
                String fieldName;
                String proposed = getSet.extractString("value", "");
                if (!proposed.isBlank()) {
                    fieldName = proposed.trim();
                } else {
                    fieldName = GetSetHelper.fieldName(mi.name());
                }
                boolean exists = builder.fields().stream().anyMatch(f -> fieldName.equals(f.name()));
                if (!exists) {
                    LOGGER.debug("Create synthetic field for {}, named {}", mi, fieldName);
                    FieldInfo syntheticField = runtime.newFieldInfo(fieldName, false, mi.returnType(), typeInfo);
                    syntheticField.builder().setSynthetic(true)
                            .addFieldModifier(runtime.fieldModifierPrivate())
                            .computeAccess()
                            .commit();
                    builder.addField(syntheticField);
                }
            }
        });
    }
}
