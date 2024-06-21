package org.e2immu.language.inspection.impl.parser;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.api.parser.ImportMap;
import org.e2immu.language.inspection.api.parser.MethodTypeParameterMap;

import java.util.*;
import java.util.stream.Collectors;

/*
Code that goes through the type hierarchy to find the candidate methods or constructors
for method/constructor resolution. Further resolution will only restrict based on parameter types.
 */
public class ListMethodAndConstructorCandidates {
    private final Runtime runtime;
    private final ImportMap importMap;

    public enum ScopeNature {
        ABSENT, STATIC, INSTANCE,
    }

    public ListMethodAndConstructorCandidates(Runtime runtime, ImportMap importMap) {
        this.importMap = importMap;
        this.runtime = runtime;
    }

    public Map<MethodTypeParameterMap, Integer> resolveConstructorInvocation(TypeInfo startingPoint,
                                                                             int parametersPresented) {
        ParameterizedType type = startingPoint.asParameterizedType(runtime);
        return resolveConstructor(type, type, parametersPresented, Map.of());
    }

    public static final int IGNORE_PARAMETER_NUMBERS = -1;

    public Map<MethodTypeParameterMap, Integer> resolveConstructor(ParameterizedType formalType,
                                                                   ParameterizedType concreteType,
                                                                   int parametersPresented,
                                                                   Map<NamedType, ParameterizedType> typeMap) {
        List<TypeInfo> types = extractTypeInfo(concreteType != null ? concreteType : formalType, typeMap);
        // there's only one situation where we can have multiple types; that's multiple type bounds; only the first one can be a class
        TypeInfo typeInfo = types.get(0);

        return typeInfo.constructors().stream()
                .filter(methodInspection -> parametersPresented == IGNORE_PARAMETER_NUMBERS ||
                                            compatibleNumberOfParameters(methodInspection, parametersPresented))
                .map(mi -> new MethodTypeParameterMapImpl(mi, typeMap))
                .collect(Collectors.toMap(mt -> mt, mt -> 1));
    }

    public void recursivelyResolveOverloadedMethods(ParameterizedType typeOfObject,
                                                    String methodName,
                                                    int parametersPresented,
                                                    boolean decrementWhenNotStatic,
                                                    Map<NamedType, ParameterizedType> typeMap,
                                                    Map<MethodTypeParameterMap, Integer> result,
                                                    ScopeNature scopeNature) {
        recursivelyResolveOverloadedMethods(typeOfObject, methodName, parametersPresented, decrementWhenNotStatic,
                typeMap, result, new HashSet<>(), new HashSet<>(), false, scopeNature, 0);
    }

    private void recursivelyResolveOverloadedMethods(ParameterizedType typeOfObject,
                                                     String methodName,
                                                     int parametersPresented,
                                                     boolean decrementWhenNotStatic,
                                                     Map<NamedType, ParameterizedType> typeMap,
                                                     Map<MethodTypeParameterMap, Integer> result,
                                                     Set<TypeInfo> visited,
                                                     Set<TypeInfo> visitedStatic,
                                                     boolean staticOnly,
                                                     ScopeNature scopeNature,
                                                     int distance) {
        List<TypeInfo> multipleTypeInfoObjects = extractTypeInfo(typeOfObject, typeMap);
        // more than one: only in the rare situation of multiple type bounds
        for (TypeInfo typeInfo : multipleTypeInfoObjects) {
            Set<TypeInfo> types = staticOnly ? visitedStatic : visited;
            if (!types.contains(typeInfo)) {
                if (!staticOnly) visited.add(typeInfo);
                visitedStatic.add(typeInfo);
                resolveOverloadedMethodsSingleType(typeInfo, staticOnly, scopeNature, methodName, parametersPresented,
                        decrementWhenNotStatic, typeMap, result, visited, visitedStatic, distance + 2);
            }
        }
        // it is possible that we find the method in one of the statically imported types... with * import
        // if the method is static, we must be talking about the same type (See Import_10).
        for (TypeInfo typeInfo : importMap.staticAsterisk()) {
            if (!visited.contains(typeInfo) && !visitedStatic.contains(typeInfo)
                && (scopeNature != ScopeNature.STATIC || typeInfo == typeOfObject.bestTypeInfo())) {
                visitedStatic.add(typeInfo);
                resolveOverloadedMethodsSingleType(typeInfo, true, scopeNature, methodName,
                        parametersPresented, decrementWhenNotStatic, typeMap, result, visited, visitedStatic,
                        distance + 1);
            }
        }
        // or import by name
        TypeInfo byName = importMap.getStaticMemberToTypeInfo(methodName);
        if (byName != null && !visited.contains(byName) && !visitedStatic.contains(byName)) {
            visitedStatic.add(byName);
            resolveOverloadedMethodsSingleType(byName, true, scopeNature, methodName,
                    parametersPresented, decrementWhenNotStatic, typeMap, result, visited, visitedStatic, distance);
        }
    }

    private void resolveOverloadedMethodsSingleType(TypeInfo typeInfo,
                                                    boolean staticOnly,
                                                    ScopeNature scopeNature,
                                                    String methodName,
                                                    int parametersPresented,
                                                    boolean decrementWhenNotStatic,
                                                    Map<NamedType, ParameterizedType> typeMap,
                                                    Map<MethodTypeParameterMap, Integer> result,
                                                    Set<TypeInfo> visited,
                                                    Set<TypeInfo> visitedStatic,
                                                    int distance) {
        boolean shallowAnalysis = false;//FIXME
        typeInfo.methodStream()//TypeInspection.Methods.THIS_TYPE_ONLY_EXCLUDE_FIELD_SAM)
                .filter(m -> m.name().equals(methodName))
                .filter(m -> !staticOnly || m.isStatic())
                .filter(m -> parametersPresented == IGNORE_PARAMETER_NUMBERS ||
                             compatibleNumberOfParameters(m, parametersPresented +
                                                             (!m.isStatic() && decrementWhenNotStatic ? -1 : 0)))
                .forEach(m -> {
                    MethodTypeParameterMap mt = new MethodTypeParameterMapImpl(m, typeMap);
                    int score = distance
                                // add a penalty for shallowly analysed, non-public methods
                                // See the java.lang.StringBuilder AbstractStringBuilder CharSequence length() problem
                                + (shallowAnalysis && !m.isPubliclyAccessible() ? 100 : 0)
                                // see e.g. MethodCall_70, where we must choose between a static and instance method
                                + (m.isStatic() && scopeNature == ScopeNature.INSTANCE ? 100 : 0);
                    result.merge(mt, score, Integer::min);
                });


        ParameterizedType parentClass = typeInfo.parentClass();
        boolean isJLO = typeInfo.isJavaLangObject();
        assert isJLO || parentClass != null :
                "Parent class of " + typeInfo.fullyQualifiedName() + " is null";
        int numInterfaces = typeInfo.interfacesImplemented().size();
        if (!isJLO) {
            recursivelyResolveOverloadedMethods(parentClass, methodName, parametersPresented, decrementWhenNotStatic,
                    joinMaps(typeMap, parentClass), result, visited, visitedStatic, staticOnly, scopeNature,
                    distance + numInterfaces + 1);
        }
        int count = 0;
        for (ParameterizedType interfaceImplemented : typeInfo.interfacesImplemented()) {
            recursivelyResolveOverloadedMethods(interfaceImplemented, methodName, parametersPresented,
                    decrementWhenNotStatic, joinMaps(typeMap, interfaceImplemented), result, visited, visitedStatic,
                    staticOnly, scopeNature, distance + count);
            ++count;
        }
        // See UtilityClass_2 for an example where we should go to the static methods of the enclosing type
        if (typeInfo.compilationUnitOrEnclosingType().isRight()) {
            // if I'm in a static subtype, I can only access the static methods of the enclosing type
            boolean onlyStatic = staticOnly || typeInfo.isStatic();
            if (onlyStatic && scopeNature != ScopeNature.INSTANCE ||
                !onlyStatic && scopeNature != ScopeNature.STATIC) {
                ParameterizedType enclosingType = typeInfo.compilationUnitOrEnclosingType().getRight().asParameterizedType(runtime);
                recursivelyResolveOverloadedMethods(enclosingType, methodName, parametersPresented, decrementWhenNotStatic,
                        joinMaps(typeMap, enclosingType), result, visited, visitedStatic,
                        onlyStatic, scopeNature, distance + numInterfaces);
            }
        }
    }


    private Map<NamedType, ParameterizedType> joinMaps(Map<NamedType, ParameterizedType> previous,
                                                       ParameterizedType target) {
        HashMap<NamedType, ParameterizedType> res = new HashMap<>(previous);
        res.putAll(target.initialTypeParameterMap(runtime));
        return res;
    }

    private boolean compatibleNumberOfParameters(MethodInfo m, int parametersPresented) {
        int declared = m.parameters().size();
        if (declared == 0) return parametersPresented == 0;
        boolean lastIsVarArgs = m.parameters().get(declared - 1).isVarArgs();
        if (lastIsVarArgs) return parametersPresented >= declared - 1;
        return parametersPresented == declared;
    }


    private List<TypeInfo> extractTypeInfo(ParameterizedType typeOfObject, Map<NamedType, ParameterizedType> typeMap) {
        TypeInfo typeInfo;
        if (typeOfObject.typeInfo() == null) {
            if (typeOfObject.typeParameter() == null) {
                throw new UnsupportedOperationException();
            }
            ParameterizedType pt = typeMap.get(typeOfObject.typeParameter());
            if (pt == null) {
                // rather than give an exception here, we replace t by the type that it extends, so that we can find those methods
                // in the case that there is no explicit extension/super, we replace it by the implicit Object
                List<ParameterizedType> typeBounds = typeOfObject.typeParameter().typeBounds();
                if (!typeBounds.isEmpty()) {
                    return typeBounds.stream().flatMap(bound -> extractTypeInfo(bound, typeMap).stream()).collect(Collectors.toList());
                } else {
                    typeInfo = runtime.objectTypeInfo();
                }
            } else {
                typeInfo = pt.typeInfo();
            }
        } else {
            typeInfo = typeOfObject.typeInfo();
        }
        assert typeInfo != null;
        // FIXME ensure that typeInfo's inspection is done, if this is bytecode inspection
        return List.of(typeInfo);
    }

}
