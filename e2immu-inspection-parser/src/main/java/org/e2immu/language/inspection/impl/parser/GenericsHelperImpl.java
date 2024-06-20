package org.e2immu.language.inspection.impl.parser;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.api.parser.GenericsHelper;
import org.e2immu.language.inspection.api.parser.MethodTypeParameterMap;

import java.util.*;
import java.util.stream.Collectors;

public record GenericsHelperImpl(Runtime runtime) implements GenericsHelper {

    @Override
    public MethodTypeParameterMap newMethodTypeParameterMap(MethodInfo methodInfo, Map<NamedType, ParameterizedType> concreteTypes) {
        return new MethodTypeParameterMapImpl(methodInfo, concreteTypes);
    }

    @Override
    public MethodTypeParameterMap findSingleAbstractMethodOfInterface(ParameterizedType parameterizedType) {
        return findSingleAbstractMethodOfInterface(parameterizedType, true);
    }

    /**
     * @param complain If false, accept that we're in a functional interface, and do not complain. Only used in the recursion.
     *                 If true, then starting point of the recursion. We need a functional interface, and will complain at the end.
     * @return the combination of method and initial type parameter map
     */
    @Override
    public MethodTypeParameterMap findSingleAbstractMethodOfInterface(ParameterizedType parameterizedType,
                                                                      boolean complain) {
        if (parameterizedType.typeInfo() == null) return null;
        MethodInfo theMethod = parameterizedType.typeInfo().singleAbstractMethod();
        if (theMethod == null) {
            if (complain) {
                throw new UnsupportedOperationException("Cannot find a single abstract method in the interface "
                                                        + parameterizedType.detailedString());
            }
            return null;
        }
        /* if theMethod comes from a superType, we need a full type parameter map,
           e.g., BinaryOperator -> BiFunction.apply, we need concrete values for T, U, V of BiFunction
         */
        Map<NamedType, ParameterizedType> map;
        if (theMethod.typeInfo().equals(parameterizedType.typeInfo())) {
            map = parameterizedType.initialTypeParameterMap(runtime);
        } else {
            map = makeTypeParameterMap(theMethod, parameterizedType, new HashSet<>());
            assert map != null; // the method must be somewhere in the hierarchy
        }
        return newMethodTypeParameterMap(theMethod, map);
    }

    private Map<NamedType, ParameterizedType> makeTypeParameterMap(MethodInfo methodInfo,
                                                                   ParameterizedType here,
                                                                   Set<TypeInfo> visited) {
        if (visited.add(here.typeInfo())) {
            if (here.typeInfo().equals(methodInfo.typeInfo())) {
                return here.initialTypeParameterMap(runtime);
            }
            for (ParameterizedType superType : here.typeInfo().interfacesImplemented()) {
                Map<NamedType, ParameterizedType> map = makeTypeParameterMap(methodInfo, superType, visited);
                if (map != null) {
                    Map<NamedType, ParameterizedType> concreteHere = here.initialTypeParameterMap(runtime);
                    Map<NamedType, ParameterizedType> newMap = new HashMap<>();
                    for (Map.Entry<NamedType, ParameterizedType> e : map.entrySet()) {
                        ParameterizedType newValue;
                        if (e.getValue().isTypeParameter()) {
                            newValue = concreteHere.get(e.getValue().typeParameter());
                        } else {
                            newValue = e.getValue();
                        }
                        newMap.put(e.getKey(), newValue);
                    }
                    return newMap;
                }
            }
        }
        return null; // not here
    }

    /*
    Starting from a formal type (List<E>), fill in a translation map given a concrete type (List<String>)
    IMPORTANT: the formal type has to have its formal parameters present, i.e., starting from TypeInfo,
    you should call this method on typeInfo.asParameterizedType(inspectionProvider) to ensure all formal
    parameters are present in this object.

    In the case of functional interfaces, this method goes via the SAM, avoiding the need of a formal implementation
    of the interface (i.e., a functional interface can have a SAM which is a function (1 argument, 1 return type)
    without explicitly implementing java.lang.function.Function)

    The third parameter decides the direction of the relation between the formal and the concrete type.
    When called from ParseMethodCallExpr, for example, 'this' is the parameter's formal parameter, and the concrete
    type has to be assignable to it.
     */

    @Override
    public Map<NamedType, ParameterizedType> translateMap(ParameterizedType formalType,
                                                          ParameterizedType concreteType,
                                                          boolean concreteTypeIsAssignableToThis) {
        if (formalType.parameters().isEmpty()) {
            if (formalType.isTypeParameter()) {
                if (formalType.arrays() > 0) {
                    if (concreteType.isFunctionalInterface()) {
                        // T[], Expression[]::new == IntFunction<Expression>
                        ParameterizedType arrayType = findSingleAbstractMethodOfInterface(concreteType)
                                .getConcreteReturnType(runtime);
                        return Map.of(formalType.typeParameter(), arrayType.copyWithFewerArrays(formalType.arrays()));
                    }
                    // T <-- String,  T[],String[] -> T <-- String, T[],String[][] -> T <- String[]
                    if (concreteType.arrays() > 0) {
                        return Map.of(formalType.typeParameter(), concreteType.copyWithFewerArrays(formalType.arrays()));
                    }
                }
                return Map.of(formalType.typeParameter(), concreteType);
            }
            // String <-- String, no translation map
            return Map.of();
        }
        assert formalType.typeInfo() != null;
        // no hope if Object or unbound wildcard is the best we have
        if (concreteType.typeInfo() == null || concreteType.isJavaLangObject()) return Map.of();

        if (formalType.isFunctionalInterface() && concreteType.isFunctionalInterface()) {
            return translationMapForFunctionalInterfaces(formalType, concreteType, concreteTypeIsAssignableToThis);
        }

        Map<NamedType, ParameterizedType> mapOfConcreteType = concreteType.initialTypeParameterMap(runtime);
        Map<NamedType, ParameterizedType> formalMap;
        if (formalType.typeInfo() == concreteType.typeInfo()) {
            // see Lambda_8 Stream<R>, R from flatmap -> Stream<T>
            formalMap = formalType.forwardTypeParameterMap(runtime);
        } else if (concreteTypeIsAssignableToThis) {
            // this is the super type (Set), concrete type is the subtype (HashSet)
            formalMap = mapInTermsOfParametersOfSuperType(concreteType.typeInfo(), formalType);
        } else {
            // concrete type is the super type, we MUST work towards the supertype!
            formalMap = mapInTermsOfParametersOfSubType(formalType.typeInfo(), concreteType);
        }
        if (formalMap == null) return mapOfConcreteType;
        return combineMaps(mapOfConcreteType, formalMap);
    }

    // TODO write tests!
    private Map<NamedType, ParameterizedType> translationMapForFunctionalInterfaces(ParameterizedType formalType,
                                                                                    ParameterizedType concreteType,
                                                                                    boolean concreteTypeIsAssignableToThis) {
        Map<NamedType, ParameterizedType> res = new HashMap<>();
        MethodTypeParameterMap methodTypeParameterMap = findSingleAbstractMethodOfInterface(formalType);
        List<ParameterInfo> methodParams = methodTypeParameterMap.methodInfo().parameters();
        MethodTypeParameterMap concreteTypeMap = findSingleAbstractMethodOfInterface(concreteType);
        List<ParameterInfo> concreteTypeAbstractParams = concreteTypeMap.methodInfo().parameters();

        if (methodParams.size() != concreteTypeAbstractParams.size()) {
            throw new UnsupportedOperationException("Have different param sizes for functional interface " +
                                                    formalType.detailedString() + " method " +
                                                    methodTypeParameterMap.methodInfo().fullyQualifiedName() + " and " +
                                                    concreteTypeMap.methodInfo().fullyQualifiedName());
        }
        for (int i = 0; i < methodParams.size(); i++) {
            ParameterizedType abstractTypeParameter = methodParams.get(i).parameterizedType();
            ParameterizedType concreteTypeParameter = concreteTypeMap.getConcreteTypeOfParameter(runtime, i);
            res.putAll(translateMap(abstractTypeParameter, concreteTypeParameter, concreteTypeIsAssignableToThis));
        }
        // and now the return type
        ParameterizedType myReturnType = methodTypeParameterMap.getConcreteReturnType(runtime);
        ParameterizedType concreteReturnType = concreteTypeMap.getConcreteReturnType(runtime);
        res.putAll(translateMap(myReturnType, concreteReturnType, concreteTypeIsAssignableToThis));
        return res;
    }

    /*
 StringMap<V> -> HashMap<K,V> -> Map<K, V>

 M2: K(map) -> K(hashmap), M1: K(hashmap) -> String
  */
    @Override
    public Map<NamedType, ParameterizedType> combineMaps(Map<NamedType, ParameterizedType> m1,
                                                         Map<NamedType, ParameterizedType> m2) {
        assert m1 != null;
        return m2.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> e.getValue().isTypeParameter() ? m1.getOrDefault(e.getValue().typeParameter(), e.getValue()) : e.getValue(),
                (v1, v2) -> {
                    throw new UnsupportedOperationException();
                }, LinkedHashMap::new));
    }

    @Override
    public Map<NamedType, ParameterizedType> mapInTermsOfParametersOfSuperType(TypeInfo ti,
                                                                               ParameterizedType superType) {
        assert superType.typeInfo() != ti;
        if (ti.parentClass() != null) {
            if (ti.parentClass().typeInfo() == superType.typeInfo()) {
                Map<NamedType, ParameterizedType> forward = superType.forwardTypeParameterMap(runtime);
                Map<NamedType, ParameterizedType> formal = ti.parentClass().initialTypeParameterMap(runtime);
                return combineMaps(forward, formal);
            }
            Map<NamedType, ParameterizedType> map = mapInTermsOfParametersOfSuperType(ti.parentClass().typeInfo(), superType);
            if (map != null) {
                return combineMaps(ti.parentClass().initialTypeParameterMap(runtime), map);
            }
        }
        for (ParameterizedType implementedInterface : ti.interfacesImplemented()) {
            if (implementedInterface.typeInfo() == superType.typeInfo()) {
                Map<NamedType, ParameterizedType> forward = superType.forwardTypeParameterMap(runtime);
                Map<NamedType, ParameterizedType> formal = implementedInterface.initialTypeParameterMap(runtime);
                return combineMaps(formal, forward);
            }
            Map<NamedType, ParameterizedType> map = mapInTermsOfParametersOfSuperType(implementedInterface.typeInfo(), superType);
            if (map != null) {
                return combineMaps(implementedInterface.initialTypeParameterMap(runtime), map);
            }
        }
        return null; // not in this branch of the recursion
    }

    // practically the duplicate of the previous, except that we should parameterize initialTypeParameterMap as well to collapse them
    @Override
    public Map<NamedType, ParameterizedType> mapInTermsOfParametersOfSubType(TypeInfo ti,
                                                                             ParameterizedType superType) {
        assert superType.typeInfo() != ti;
        if (ti.parentClass() != null) {
            if (ti.parentClass().typeInfo() == superType.typeInfo()) {
                return ti.parentClass().forwardTypeParameterMap(runtime);
            }
            Map<NamedType, ParameterizedType> map = mapInTermsOfParametersOfSubType(ti.parentClass().typeInfo(),
                    superType);
            if (map != null) {
                return combineMaps(map, ti.parentClass().forwardTypeParameterMap(runtime));
            }
        }
        for (ParameterizedType implementedInterface : ti.interfacesImplemented()) {
            if (implementedInterface.typeInfo() == superType.typeInfo()) {
                return implementedInterface.forwardTypeParameterMap(runtime);
            }
            Map<NamedType, ParameterizedType> map = mapInTermsOfParametersOfSubType(implementedInterface.typeInfo(),
                    superType);
            if (map != null) {
                return combineMaps(map, implementedInterface.forwardTypeParameterMap(runtime));
            }
        }
        return null; // not in this branch of the recursion
    }
}
