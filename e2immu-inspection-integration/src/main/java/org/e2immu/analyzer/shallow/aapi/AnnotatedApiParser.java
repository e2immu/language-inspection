package org.e2immu.analyzer.shallow.aapi;

import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.StringConstant;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.SourceTypes;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class AnnotatedApiParser implements AnnotationProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotatedApiParser.class);

    private record Data(List<AnnotationExpression> annotations) {
    }

    private final Map<Info, Data> infoMap = new HashMap<>();
    private final JavaInspector javaInspector;
    private int warnings;
    private int annotatedTypes;
    private int annotations;

    public AnnotatedApiParser() {
        javaInspector = new JavaInspectorImpl();
    }

    public void initialize(List<String> addToClasspath, List<String> sourceDirs, List<String> packageList) throws IOException {
        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder()
                .addClassPath(InputConfigurationImpl.DEFAULT_CLASSPATH);
        sourceDirs.forEach(builder::addSources);
        packageList.forEach(builder::addRestrictSourceToPackages);
        addToClasspath.forEach(builder::addClassPath);
        InputConfiguration inputConfiguration = builder.build();
        javaInspector.initialize(inputConfiguration);
        javaInspector.sourceTypes().visit(new String[]{}, (parts, list) -> {
            list.forEach(this::load);
        });
        LOGGER.info("Finished parsing, annotated {} types, counted {} annotations, issued {} warning(s)",
                annotatedTypes, annotations, warnings);
    }

    private void load(TypeInfo typeInfo) {
        javaInspector.parse(typeInfo);
        FieldInfo packageName = typeInfo.getFieldByName("PACKAGE_NAME", false);
        if (packageName == null) {
            LOGGER.info("Ignoring class {}, has no PACKAGE_NAME field", typeInfo);
            return;
        }
        String apiPackage;
        if (packageName.initializer() instanceof StringConstant sc) {
            apiPackage = sc.constant();
        } else {
            LOGGER.info("Ignoring class {}, PACKAGE_NAME field has not been assigned a String literal", typeInfo);
            return;
        }
        LOGGER.debug("Starting AAPI inspection of {}, in API package {}", typeInfo, apiPackage);
        typeInfo.subTypes().forEach(st -> inspect(apiPackage, st));
    }

    private void inspect(String apiPackage, TypeInfo typeInfo) {
        if (typeInfo.simpleName().endsWith("$")) {
            String simpleNameWithoutDollar = typeInfo.simpleName().substring(0, typeInfo.simpleName().length() - 1);
            String fqn = apiPackage + "." + simpleNameWithoutDollar;
            TypeInfo targetType = javaInspector.compiledTypesManager().getOrLoad(fqn);
            if (targetType != null) {
                annotatedTypes++;
                transferAnnotations(typeInfo, targetType);
            } else {
                warnings++;
                LOGGER.warn("Ignoring type '{}', cannot load it.", fqn);
            }
        } else {
            LOGGER.warn("Ignoring type '{}', name does not end in $.", typeInfo);
            warnings++;
        }
    }

    private void transferAnnotations(TypeInfo sourceType, TypeInfo targetType) {
        Data typeData = new Data(sourceType.annotations());
        annotations += sourceType.annotations().size();
        infoMap.put(targetType, typeData);

        for (TypeInfo subType : sourceType.subTypes()) {
            TypeInfo targetSubType = targetType.findSubType(subType.simpleName(), false);
            if (targetSubType != null) {
                transferAnnotations(subType, targetSubType);
            } else {
                LOGGER.warn("Ignoring subtype '{}', cannot find it in the target type '{}'",
                        subType.simpleName(), targetType);
                warnings++;
            }
        }
        for (MethodInfo sourceMethod : sourceType.methods()) {
            if (!sourceMethod.name().contains("$")) {
                MethodInfo targetMethod = findTargetMethod(targetType, sourceMethod);
                if (targetMethod != null) {
                    annotations += sourceMethod.annotations().size();
                    Data methodData = new Data(sourceMethod.annotations());
                    infoMap.put(targetMethod, methodData);
                } else {
                    LOGGER.warn("Ignoring method '{}', not found in target type '{}'", sourceMethod, targetType);
                    ++warnings;
                }
            } // else companion method, not implemented at the moment
        }
        for (MethodInfo sourceMethod : sourceType.constructors()) {
            MethodInfo targetMethod = findTargetConstructor(targetType, sourceMethod);
            if (targetMethod != null) {
                annotations += sourceMethod.annotations().size();
                Data methodData = new Data(sourceMethod.annotations());
                infoMap.put(targetMethod, methodData);
            } else {
                LOGGER.warn("Ignoring constructor '{}', not found in target type '{}'", sourceMethod, targetType);
                ++warnings;
            }
        }
    }

    private MethodInfo findTargetConstructor(TypeInfo targetType, MethodInfo sourceMethod) {
        int n = sourceMethod.parameters().size();
        for (MethodInfo candidate : targetType.constructors()) {
            if (candidate.parameters().size() == n && sameParameterTypes(candidate, sourceMethod)) {
                return candidate;
            }
        }
        return null;
    }

    private MethodInfo findTargetMethod(TypeInfo targetType, MethodInfo sourceMethod) {
        int n = sourceMethod.parameters().size();
        for (MethodInfo candidate : targetType.methods()) {
            if (candidate.parameters().size() == n
                && candidate.name().equals(sourceMethod.name())
                && sameParameterTypes(candidate, sourceMethod)) {
                return candidate;
            }
        }
        return null; // cannot find the method, we'll NOT be looking at a supertype, since we cannot add a copy
    }

    private boolean sameParameterTypes(MethodInfo candidate, MethodInfo sourceMethod) {
        Iterator<ParameterInfo> it = candidate.parameters().iterator();
        for (ParameterInfo pi : sourceMethod.parameters()) {
            assert it.hasNext();
            ParameterInfo pi2 = it.next();
            if (!sameType(pi.parameterizedType(), pi2.parameterizedType())) return false;
        }
        return true;
    }

    private boolean sameType(ParameterizedType pt1, ParameterizedType pt2) {
        if (pt1.typeInfo() != null) return pt1.arrays() == pt2.arrays() && pt1.typeInfo() == pt2.typeInfo();
        return (pt1.typeParameter() == null) == (pt2.typeParameter() == null);
    }


    // for testing
    public SourceTypes sourceTypes() {
        return javaInspector.sourceTypes();
    }
    // for testing
    public Runtime runtime() {
        return javaInspector.runtime();
    }

    @Override
    public List<AnnotationExpression> annotations(Info info) {
        Data data = infoMap.get(info);
        if (data != null) return data.annotations;
        return List.of();
    }

    public int getWarnings() {
        return warnings;
    }
}
