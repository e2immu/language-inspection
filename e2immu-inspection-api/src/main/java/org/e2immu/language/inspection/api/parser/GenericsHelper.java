package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.Map;

public interface GenericsHelper {

    MethodTypeParameterMap newMethodTypeParameterMap(MethodInfo methodInfo, Map<NamedType, ParameterizedType> concreteTypes);

    /*
    StringMap<V> -> HashMap<K,V> -> Map<K, V>

    M2: K(map) -> K(hashmap), M1: K(hashmap) -> String
    */
    Map<NamedType, ParameterizedType> combineMaps(Map<NamedType,
            ParameterizedType> m1, Map<NamedType,
            ParameterizedType> m2);

    MethodTypeParameterMap findSingleAbstractMethodOfInterface(ParameterizedType parameterizedType);

    MethodTypeParameterMap findSingleAbstractMethodOfInterface(
            ParameterizedType parameterizedType,
            boolean complain);

    // practically the duplicate of the previous, except that we should parameterize initialTypeParameterMap as well to collapse them
    Map<NamedType, ParameterizedType> mapInTermsOfParametersOfSubType(TypeInfo ti,
                                                                      ParameterizedType superType);

    Map<NamedType, ParameterizedType> mapInTermsOfParametersOfSuperType(TypeInfo ti,
                                                                        ParameterizedType superType);

    Map<NamedType, ParameterizedType> translateMap(ParameterizedType formalType,
                                                   ParameterizedType concreteType,
                                                   boolean concreteTypeIsAssignableToThis);
}
