package org.e2immu.language.inspection.impl.parser;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.type.TypeParameter;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.inspection.api.parser.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class MethodResolutionImpl implements MethodResolution {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodResolutionImpl.class);
    private final Runtime runtime;
    private final GenericsHelper genericsHelper;
    private final HierarchyHelper hierarchyHelper;
    private final int notAssignable;
    private final ListMethodAndConstructorCandidates listMethodAndConstructorCandidates;

    public MethodResolutionImpl(Runtime runtime, ImportMap importMap) {
        this.runtime = runtime;
        this.genericsHelper = new GenericsHelperImpl(runtime);
        this.hierarchyHelper = new HierarchyHelperImpl();
        notAssignable = runtime.isNotAssignable();
        listMethodAndConstructorCandidates = new ListMethodAndConstructorCandidates(runtime, importMap);
    }


    @Override
    public Set<ParameterizedType> computeScope(Context context,
                                               String index,
                                               String methodName,
                                               Object unparsedScope,
                                               List<Object> unparsedArguments) {
        ListMethodAndConstructorCandidates.Scope scope = listMethodAndConstructorCandidates
                .computeScope(context.parseHelper(), context, index, unparsedScope, TypeParameterMap.EMPTY);
        int numArguments = unparsedArguments.size();
        Map<MethodTypeParameterMap, Integer> methodCandidates = initialMethodCandidates(scope, numArguments, methodName);

        FilterResult filterResult = filterMethodCandidatesInErasureMode(context, methodCandidates, unparsedArguments);
        if (methodCandidates.size() > 1) {
            trimMethodsWithBestScore(methodCandidates, filterResult.compatibilityScore);
            if (methodCandidates.size() > 1) {
                trimVarargsVsMethodsWithFewerParameters(methodCandidates);
            }
        }
        sortRemainingCandidatesByShallowPublic(methodCandidates);

        if (methodCandidates.isEmpty()) {
            throw new RuntimeException("Have no method candidates remaining in erasure mode for "
                                       + methodName + ", " + numArguments);
        }

        Set<ParameterizedType> types;
        if (scope.expression() != null
            && !(scope.expression() instanceof ErasedExpression)
            && scope.expression().parameterizedType().arrays() > 0
            && "clone".equals(methodName)) {
            /* this condition is hyper-specialized (see MethodCall_54; but the alternative would be to return JLO,
               and that causes problems along the way
             */
            types = Set.of(scope.expression().parameterizedType());
        } else {
            types = methodCandidates.keySet().stream()
                    .map(mc -> erasureReturnType(mc, filterResult, scope))
                    .collect(Collectors.toUnmodifiableSet());
        }
        return types;
    }

    @Override
    public Expression resolveMethod(Context context, List<Comment> comments, Source source, String index, ForwardType forwardType,
                                    String methodName, Object unparsedScope, List<Object> unparsedArguments) {
        ListMethodAndConstructorCandidates.Scope scope = listMethodAndConstructorCandidates
                .computeScope(context.parseHelper(), context, index, unparsedScope, TypeParameterMap.EMPTY);
        int numArguments = unparsedArguments.size();
        Map<MethodTypeParameterMap, Integer> methodCandidates = initialMethodCandidates(scope, numArguments, methodName);
        if (methodCandidates.isEmpty()) {
            throw new Summary.ParseException(context.info(), "No method candidates for " + methodName
                                                             + ", " + numArguments + " arg(s)");
        }
        TypeParameterMap extra = forwardType.extra().merge(scope.typeParameterMap());
        Candidate candidate = chooseCandidateAndEvaluateCall(context, index, methodName, methodCandidates,
                unparsedArguments, forwardType.type(), extra);

        if (candidate == null) {
            throw new Summary.ParseException(context.info(), "Failed to find a unique method candidate");
        }
        LOGGER.debug("Resulting method is {}", candidate.method.methodInfo());

        boolean scopeIsThis = scope.expression() instanceof VariableExpression ve && ve.variable() instanceof This;
        Expression newScope = scope.ensureExplicit(runtime, hierarchyHelper, candidate.method.methodInfo(),
                scopeIsThis, context.enclosingType());
        ParameterizedType returnType = candidate.returnType(runtime, context.enclosingType().primaryType(), extra);
        LOGGER.debug("Concrete return type of {} is {}", methodName, returnType.detailedString());

        return runtime.newMethodCallBuilder()
                .setSource(source).addComments(comments)
                .setObjectIsImplicit(scope.objectIsImplicit())
                .setObject(newScope)
                .setMethodInfo(candidate.method.methodInfo())
                .setConcreteReturnType(returnType)
                .setParameterExpressions(candidate.newParameterExpressions).build();
    }

    record Candidate(List<Expression> newParameterExpressions,
                     Map<NamedType, ParameterizedType> mapExpansion,
                     MethodTypeParameterMap method) {

        ParameterizedType returnType(Runtime runtime,
                                     TypeInfo primaryType,
                                     TypeParameterMap extra) {
            ParameterizedType pt = mapExpansion.isEmpty()
                    ? method.getConcreteReturnType(runtime)
                    : method.expand(runtime, primaryType, mapExpansion).getConcreteReturnType(runtime);
            ParameterizedType withExtra = pt.applyTranslation(runtime, extra.map());
            // See TypeParameter_4
            return withExtra.isUnboundWildcard() ? runtime.objectParameterizedType() : withExtra;
        }
    }

    private Map<MethodTypeParameterMap, Integer> initialMethodCandidates(ListMethodAndConstructorCandidates.Scope scope,
                                                                         int numArguments,
                                                                         String methodName) {
        Map<MethodTypeParameterMap, Integer> methodCandidates = new HashMap<>();
        listMethodAndConstructorCandidates.recursivelyResolveOverloadedMethods(scope.type(), methodName,
                numArguments, false, scope.typeParameterMap().map(), methodCandidates,
                scope.nature());
        if (methodCandidates.isEmpty()) {
            throw new RuntimeException("No candidates at all for method name " + methodName + ", "
                                       + numArguments + " args in type " + scope.type().detailedString());
        }
        return methodCandidates;
    }

    private ParameterizedType erasureReturnType(MethodTypeParameterMap mc, FilterResult filterResult,
                                                ListMethodAndConstructorCandidates.Scope scope) {
        MethodInfo candidate = mc.methodInfo();
        TypeParameterMap map0 = filterResult.typeParameterMap(runtime, candidate);
        TypeParameterMap map1 = map0.merge(scope.typeParameterMap());
        TypeInfo methodType = candidate.typeInfo();
        TypeInfo scopeType = scope.type().bestTypeInfo();
        TypeParameterMap merged;
        if (scopeType != null && !methodType.equals(scopeType)) {
            // method is defined in a super-type, so we need an additional translation
            ParameterizedType superType = methodType.asParameterizedType(runtime);
            Map<NamedType, ParameterizedType> sm = genericsHelper.mapInTermsOfParametersOfSuperType(scopeType, superType);
            merged = sm == null ? map1 : map1.merge(new TypeParameterMap(sm));
        } else {
            merged = map1;
        }
        ParameterizedType returnType = candidate.returnType();
        Map<NamedType, ParameterizedType> map2 = merged.map();
        // IMPROVE at some point, compare to mc.method().concreteType; redundant code?
        return returnType.applyTranslation(runtime, map2);
    }


    Candidate chooseCandidateAndEvaluateCall(Context context,
                                             String index,
                                             String methodName,
                                             Map<MethodTypeParameterMap, Integer> methodCandidates,
                                             List<Object> unparsedArguments,
                                             ParameterizedType returnType,
                                             TypeParameterMap extra) {

        Map<Integer, Expression> evaluatedExpressions = new TreeMap<>();
        int i = 0;
        ForwardType forward = context.erasureForwardType();
        for (Object argument : unparsedArguments) {
            Expression evaluated = context.resolver().parseHelper().parseExpression(context, index, forward, argument);
            evaluatedExpressions.put(i++, evaluated);
        }

        FilterResult filterResult = filterCandidatesByParameters(methodCandidates, evaluatedExpressions, extra, false);

        // now we need to ensure that there is only 1 method left, but, there can be overloads and
        // methods with implicit type conversions, varargs, etc. etc.
        if (methodCandidates.isEmpty()) {
            return noCandidatesError(methodName, filterResult.evaluatedExpressions);
        }

        if (methodCandidates.size() > 1) {
            trimMethodsWithBestScore(methodCandidates, filterResult.compatibilityScore);
            if (methodCandidates.size() > 1) {
                trimVarargsVsMethodsWithFewerParameters(methodCandidates);
            }
        }
        List<MethodTypeParameterMap> sorted = sortRemainingCandidatesByShallowPublic(methodCandidates);
        if (sorted.size() > 1) {
            multipleCandidatesError(methodName, methodCandidates, filterResult.evaluatedExpressions);
        }
        MethodTypeParameterMap method = sorted.get(0);
        LOGGER.debug("Found method {}", method.methodInfo());

        List<Expression> newParameterExpressions = reEvaluateErasedExpression(context, index, unparsedArguments,
                returnType, extra, methodName, filterResult.evaluatedExpressions, method);
        Map<NamedType, ParameterizedType> mapExpansion = computeMapExpansion(method, newParameterExpressions,
                returnType, context.enclosingType().primaryType());
        return new Candidate(newParameterExpressions, mapExpansion, method);
    }

    private void multipleCandidatesError(String methodName,
                                         Map<MethodTypeParameterMap, Integer> methodCandidates,
                                         Map<Integer, Expression> evaluatedExpressions) {
        LOGGER.error("Multiple candidates for {}", methodName);
        methodCandidates.forEach((m, d) -> LOGGER.error(" -- {}", m.methodInfo()));
        LOGGER.error("{} Evaluated expressions:", evaluatedExpressions.size());
        evaluatedExpressions.forEach((i, e) -> LOGGER.error(" -- index {}: {}, {}, {}", i, e, e.getClass(),
                e instanceof ErasedExpression ? "-" : e.parameterizedType().toString()));
        throw new UnsupportedOperationException("Multiple candidates");
    }

    private Map<NamedType, ParameterizedType> computeMapExpansion(MethodTypeParameterMap method,
                                                                  List<Expression> newParameterExpressions,
                                                                  ParameterizedType forwardedReturnType,
                                                                  TypeInfo primaryType) {
        Map<NamedType, ParameterizedType> mapExpansion = new HashMap<>();
        // fill in the map expansion, deal with variable arguments!
        int i = 0;
        List<ParameterInfo> formalParameters = method.methodInfo().parameters();

        for (Expression expression : newParameterExpressions) {
            LOGGER.debug("Examine parameter {}", i);
            ParameterizedType concreteParameterType;
            ParameterizedType formalParameterType;
            ParameterInfo formalParameter = formalParameters.get(i);
            if (formalParameters.size() - 1 == i && formalParameter.isVarArgs()) {
                formalParameterType = formalParameter.parameterizedType().copyWithOneFewerArrays();
                if (newParameterExpressions.size() > formalParameters.size()
                    || formalParameterType.arrays() == expression.parameterizedType().arrays()) {
                    concreteParameterType = expression.parameterizedType();
                } else {
                    concreteParameterType = expression.parameterizedType().copyWithOneFewerArrays();
                    assert formalParameterType.isAssignableFrom(runtime, concreteParameterType);
                }
            } else {
                formalParameterType = formalParameters.get(i).parameterizedType();
                concreteParameterType = expression.parameterizedType();
            }
            Map<NamedType, ParameterizedType> translated = genericsHelper.translateMap(formalParameterType,
                    concreteParameterType, true);
            ParameterizedType concreteTypeInMethod = method.getConcreteTypeOfParameter(runtime, i);

            translated.forEach((k, v) -> {
                // we can go in two directions here.
                // either the type parameter gets a proper value by the concreteParameterType, or the concreteParameter type should
                // agree with the concrete types map in the method candidate. It is quite possible that concreteParameterType == ParameterizedType.NULL,
                // and then the value in the map should prevail
                ParameterizedType valueToAdd;
                if (betterDefinedThan(concreteTypeInMethod, v)) {
                    valueToAdd = concreteTypeInMethod;
                } else {
                    valueToAdd = v;
                }
                /* Example: Ecoll -> String, in case the formal parameter was Collection<E>, and the concrete Set<String>
                Now if Ecoll is a method parameter, it needs linking to the

                 */
                if (!mapExpansion.containsKey(k)) {
                    mapExpansion.put(k, valueToAdd);
                }
            });
            i++;
            if (i >= formalParameters.size()) break; // varargs... we have more than there are
        }

        // finally, look at the return type
        ParameterizedType formalReturnType = method.methodInfo().returnType();
        if (formalReturnType.isTypeParameter() && forwardedReturnType != null) {
            mapExpansion.merge(formalReturnType.typeParameter(), forwardedReturnType,
                    (ptOld, ptNew) -> ptOld.mostSpecific(runtime, primaryType, ptNew));
        }

        return mapExpansion;
    }

    private static boolean betterDefinedThan(ParameterizedType pt1, ParameterizedType pt2) {
        return (pt1.typeParameter() != null || pt1.typeInfo() != null) && pt2.typeParameter() == null && pt2.typeInfo() == null;
    }


    private List<Expression> reEvaluateErasedExpression(Context context,
                                                        String index,
                                                        List<Object> expressions,
                                                        ParameterizedType outsideContext,
                                                        TypeParameterMap extra,
                                                        String methodName,
                                                        Map<Integer, Expression> evaluatedExpressions,
                                                        MethodTypeParameterMap method) {
        Expression[] newParameterExpressions = new Expression[evaluatedExpressions.size()];
        TypeParameterMap cumulative = extra;
        List<Integer> positionsToDo = new ArrayList<>(evaluatedExpressions.size());
        List<ParameterInfo> parameters = method.methodInfo().parameters();

        for (int i = 0; i < expressions.size(); i++) {
            Expression e = evaluatedExpressions.get(i);
            assert e != null;
            if (!erasureTypes(e).isEmpty()) {
                positionsToDo.add(i);
            } else {
                newParameterExpressions[i] = e;
                Map<NamedType, ParameterizedType> learned = e.parameterizedType().initialTypeParameterMap(runtime);
                ParameterizedType formal = i < parameters.size() ? parameters.get(i).parameterizedType() :
                        parameters.get(parameters.size() - 1).parameterizedType().copyWithOneFewerArrays();
                Map<NamedType, ParameterizedType> inMethod = formal.forwardTypeParameterMap(runtime);
                Map<NamedType, ParameterizedType> combined = genericsHelper.combineMaps(learned, inMethod);
                if (!combined.isEmpty()) {
                    cumulative = cumulative.merge(new TypeParameterMap(combined));
                }
                if (formal.typeParameter() != null) {
                    Map<NamedType, ParameterizedType> map = Map.of(formal.typeParameter(), e.parameterizedType().copyWithoutArrays());
                    cumulative = cumulative.merge(new TypeParameterMap(map));
                }
            }
        }

        for (int i : positionsToDo) {
            Expression e = evaluatedExpressions.get(i);
            assert e != null;

            LOGGER.debug("Reevaluating erased expression on {}, pos {}", methodName, i);
            ForwardType newForward = determineForwardReturnTypeInfo(method, i, outsideContext, cumulative);

            Expression reParsed = context.resolver().parseHelper().parseExpression(context, index, newForward,
                    expressions.get(i));
            assert erasureTypes(reParsed).isEmpty();
            newParameterExpressions[i] = reParsed;

            Map<NamedType, ParameterizedType> learned = reParsed.parameterizedType().initialTypeParameterMap(runtime);
            if (!learned.isEmpty()) {
                cumulative = cumulative.merge(new TypeParameterMap(learned));
            }
            ParameterInfo pi = parameters.get(Math.min(i, parameters.size() - 1));
            if (pi.parameterizedType().hasTypeParameters()) {
                // try to reconcile the type parameters with the ones in reParsed, see Lambda_16
                Map<NamedType, ParameterizedType> forward = pi.parameterizedType().forwardTypeParameterMap(runtime);
                if (!forward.isEmpty()) {
                    cumulative = cumulative.merge(new TypeParameterMap(forward));
                }
            }
        }
        return Arrays.stream(newParameterExpressions).toList();
    }

    private Candidate noCandidatesError(String methodName, Map<Integer, Expression> evaluatedExpressions) {
        LOGGER.error("Evaluated expressions for {}: ", methodName);
        evaluatedExpressions.forEach((i, expr) -> LOGGER.error("  {} = {}", i, expr));
        LOGGER.error("No candidate found for {}", methodName);
        return null;
    }

    private record FilterResult(Map<Integer, Expression> evaluatedExpressions,
                                Map<MethodInfo, Integer> compatibilityScore) {

        // See Lambda_6, Lambda_13: connect type of evaluated argument result to formal parameter type
        public TypeParameterMap typeParameterMap(Runtime runtime,
                                                 MethodInfo candidate) {
            Map<NamedType, ParameterizedType> result = new HashMap<>();
            int i = 0;
            for (ParameterInfo parameterInfo : candidate.parameters()) {
                if (i >= evaluatedExpressions.size()) break;
                Expression expression = evaluatedExpressions.get(i);
                // we have a match between the return type of the expression, and the type of the parameter
                // expression: MethodCallErasure, with Collector<T,?,Set<T>> as concrete type
                // parameter type: Collector<? super T,A,R>   (T belongs to Stream, A,R to the collect method)
                // we want to add R --> Set<T> to the type map
                Map<NamedType, ParameterizedType> map = new HashMap<>();
                Set<ParameterizedType> erasureTypes = erasureTypes(expression);
                if (!erasureTypes.isEmpty()) {
                    for (ParameterizedType pt : erasureTypes) {
                        map.putAll(pt.initialTypeParameterMap(runtime));
                    }
                } else {
                    map.putAll(expression.parameterizedType().initialTypeParameterMap(runtime));
                }
                // we now have R as #2 in Collector mapped to Set<T>, and we need to replace that by the
                // actual type parameter of the formal type of parameterInfo
                //result.putAll( parameterInfo.parameterizedType.translateMap(typeContext, map));
                int j = 0;
                boolean isVarargs = parameterInfo.isVarArgs();
                for (ParameterizedType tp : parameterInfo.parameterizedType().parameters()) {
                    if (tp.typeParameter() != null) {
                        int index = j;
                        map.entrySet().stream()
                                .filter(e -> e.getKey() instanceof TypeParameter t && t.getIndex() == index)
                                // we do not add to the map when the result is one type parameter to the next (MethodCall_19)
                                .filter(e -> e.getValue().bestTypeInfo() != null)
                                .map(Map.Entry::getValue)
                                .findFirst()
                                .ifPresent(inMap -> {
                                    // see MethodCall_60,_61,_62,_63 for the array count computation
                                    ParameterizedType target = inMap.copyWithArrays(inMap.arrays() - tp.arrays());
                                    result.merge(tp.typeParameter(), target, ParameterizedType::bestDefined);
                                });
                    }
                    j++;
                }
                if (parameterInfo.parameterizedType().isTypeParameter() && erasureTypes.isEmpty()) {
                    // see MethodCall_48; MethodCall_3 shows why we omit ErasureExpression
                    // see MethodCall_60,_61,_62,_63 for the array count computation
                    boolean oneFewer = expression.parameterizedType().arrays() == parameterInfo.parameterizedType().arrays() - 1;
                    int paramArrays = parameterInfo.parameterizedType().arrays() - (isVarargs && oneFewer ? 1 : 0);
                    int arrays = expression.parameterizedType().arrays() - paramArrays;
                    ParameterizedType target = expression.parameterizedType().copyWithArrays(arrays);
                    result.merge(parameterInfo.parameterizedType().typeParameter(), target, ParameterizedType::bestDefined);
                }
                i++;
            }
            return new TypeParameterMap(result);
        }
    }


    private FilterResult filterCandidatesByParameters(Map<MethodTypeParameterMap, Integer> methodCandidates,
                                                      Map<Integer, Expression> evaluatedExpressions,
                                                      TypeParameterMap typeParameterMap,
                                                      boolean explain) {
        Map<Integer, Set<ParameterizedType>> acceptedErasedTypes =
                evaluatedExpressions.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e ->
                        erasureTypes(e.getValue()).stream()
                                .map(pt -> pt.applyTranslation(runtime, typeParameterMap.map()))
                                .collect(Collectors.toUnmodifiableSet())));

        Map<Integer, ParameterizedType> acceptedErasedTypesCombination = null;
        Map<MethodInfo, Integer> compatibilityScore = new HashMap<>();

        for (Map.Entry<MethodTypeParameterMap, Integer> entry : methodCandidates.entrySet()) {
            int sumScore = 0;
            boolean foundCombination = true;
            List<ParameterInfo> parameters = entry.getKey().methodInfo().parameters();
            int pos = 0;
            Map<Integer, ParameterizedType> thisAcceptedErasedTypesCombination = new TreeMap<>();
            for (ParameterInfo parameterInfo : parameters) {

                Set<ParameterizedType> acceptedErased = acceptedErasedTypes.get(pos);
                ParameterizedType bestAcceptedType = null;
                int bestCompatible = Integer.MIN_VALUE;

                int varargsPenalty;
                ParameterizedType formalType;
                if (parameterInfo.isVarArgs()) {
                    if (acceptedErased == null) {
                        // the parameter is a varargs, and we have the empty array
                        assert parameters.size() == evaluatedExpressions.size() + 1;
                        break;
                    }
                    if (pos == parameters.size() - 1) {
                        // this one can be either the array matching the type, an element of the array
                        ParameterizedType arrayType = parameterInfo.parameterizedType();

                        // here comes a bit of code duplication...
                        for (ParameterizedType actualType : acceptedErased) {

                            int compatible;
                            if (isUnboundMethodTypeParameter(actualType) && actualType.arrays() == arrayType.arrays()) {
                                compatible = 5;
                            } else {
                                compatible = callIsAssignableFrom(actualType, arrayType, explain);
                            }
                            if (compatible >= 0 && (bestCompatible == Integer.MIN_VALUE || compatible < bestCompatible)) {
                                bestCompatible = compatible;
                                bestAcceptedType = actualType;
                            }
                        }

                        // and break off the code duplication, because we cannot set foundCombination to false
                        if (bestCompatible >= 0) {
                            sumScore += bestCompatible;
                            thisAcceptedErasedTypesCombination.put(pos, bestAcceptedType);
                            break;
                        } // else: we have another one to try!
                        // see Constructor_18 for example where varargsPenalty is important
                        varargsPenalty = 50;
                    } else {
                        varargsPenalty = 0;
                    }
                    formalType = parameterInfo.parameterizedType().copyWithOneFewerArrays();
                } else {
                    assert acceptedErased != null;
                    formalType = parameterInfo.parameterizedType();
                    varargsPenalty = 0;
                }


                for (ParameterizedType actualType : acceptedErased) {
                    int penaltyForReturnType = computePenaltyForReturnType(actualType, formalType);
                    for (ParameterizedType actualTypeReplaced : actualType.replaceByTypeBounds()) {
                        for (ParameterizedType formalTypeReplaced : formalType.replaceByTypeBounds()) {

                            boolean paramIsErasure = evaluatedExpressions.get(pos) instanceof ErasedExpression;
                            int compatible;
                            if (actualTypeReplaced.isTypeOfNullConstant()) {
                                // compute the distance to Object, so that the nearest one loses. See MethodCall_66
                                // IMPROVE why 100?
                                if (formalTypeReplaced.isPrimitiveExcludingVoid()) {
                                    compatible = -1; // MethodCall_69
                                } else {
                                    // note: always assignable! array penalties easily go into the 100's so 1000 seems safe
                                    ParameterizedType objectPt = runtime.objectParameterizedType();
                                    int c = callIsAssignableFrom(formalTypeReplaced, objectPt, explain);
                                    assert c >= 0;
                                    // See MethodCall_66, resp. _74 for the '-' and the '1000'
                                    compatible = varargsPenalty + 1000 - c;
                                }
                            } else if (paramIsErasure && actualTypeReplaced != actualType) {
                                /*
                                 See 'method' call in MethodCall_32; this feels like a hack.
                                 Map.get(e.getKey()) call in MethodCall_37 shows the opposite direction; so we do Max.
                                 Feels even more like a hack.
                                 */
                                compatible = Math.max(callIsAssignableFrom(formalTypeReplaced, actualTypeReplaced, explain),
                                        callIsAssignableFrom(actualTypeReplaced, formalTypeReplaced, explain));
                            } else {
                                int c = callIsAssignableFrom(actualTypeReplaced, formalTypeReplaced, explain);
                                compatible = c < 0 ? c : varargsPenalty + c;
                            }

                            if (compatible >= 0 && (bestCompatible == Integer.MIN_VALUE
                                                    || (compatible + penaltyForReturnType) < bestCompatible)) {
                                bestCompatible = compatible + penaltyForReturnType;
                                bestAcceptedType = actualType;
                            }
                        }
                    }
                }
                if (bestCompatible < 0) {
                    foundCombination = false;
                } else {
                    sumScore += bestCompatible;
                    thisAcceptedErasedTypesCombination.put(pos, bestAcceptedType);
                }
                pos++;
            }
            if (!foundCombination) {
                sumScore = -1; // to be removed immediately
            } else {
                int varargsSkipped = Math.abs(evaluatedExpressions.size() - parameters.size());
                int methodDistance = entry.getValue();
                sumScore += 100 * methodDistance + 10 * varargsSkipped;
                if (acceptedErasedTypesCombination == null) {
                    acceptedErasedTypesCombination = thisAcceptedErasedTypesCombination;
                } else if (!acceptedErasedTypesCombination.equals(thisAcceptedErasedTypesCombination)) {
                    LOGGER.debug("Looks like multiple, different, combinations? {} to {}", acceptedErasedTypesCombination,
                            thisAcceptedErasedTypesCombination);
                }
            }
            compatibilityScore.put(entry.getKey().methodInfo(), sumScore);
        }

        // remove those with a negative compatibility score
        methodCandidates.entrySet().removeIf(e -> {
            int score = compatibilityScore.get(e.getKey().methodInfo());
            return score < 0;
        });

        return new FilterResult(evaluatedExpressions, compatibilityScore);
    }

    private int computePenaltyForReturnType(ParameterizedType actualType,
                                            ParameterizedType formalType) {
        if (actualType.typeInfo() == null || formalType.typeInfo() == null) return 0;
        MethodInfo actual = actualType.typeInfo().singleAbstractMethod();
        if (actual == null) return 0; // not worth the effort
        MethodInfo formal = formalType.typeInfo().singleAbstractMethod();
        if (formal == null) return 0;
        if (actual.isVoid() && !formal.isVoid()) return notAssignable;
        // we have to have a small penalty in the other direction, to give preference to a Consumer when a Function is competing
        if (!actual.isVoid() && formal.isVoid()) return 5;
        return 0;
    }

    private boolean isUnboundMethodTypeParameter(ParameterizedType actualType) {
        return actualType.typeParameter() != null
               && actualType.typeParameter().isMethodTypeParameter()
               && actualType.typeParameter().typeBounds().isEmpty();
    }

    private FilterResult filterMethodCandidatesInErasureMode(Context context,
                                                             Map<MethodTypeParameterMap, Integer> methodCandidates,
                                                             List<Object> expressions) {
        Map<Integer, Expression> evaluatedExpressions = new HashMap<>();
        Map<MethodInfo, Integer> compatibilityScore = new HashMap<>();
        int pos = 0;
        while (!methodCandidates.isEmpty() && evaluatedExpressions.size() < expressions.size()) {
            ForwardType fwd = context.erasureForwardType();
            Expression evaluatedExpression = context.parseHelper().parseExpression(context, "", fwd,
                    expressions.get(pos));
            evaluatedExpressions.put(pos, Objects.requireNonNull(evaluatedExpression));
            filterCandidatesByParameter(evaluatedExpression, pos, methodCandidates, compatibilityScore);
            pos++;
        }
        return new FilterResult(evaluatedExpressions, compatibilityScore);
    }


    /**
     * StringBuilder.length() is in public interface CharSequence and in the private type AbstractStringBuilder.
     * We prioritise the CharSequence version, because that one can be annotated using annotated APIs.
     *
     * @param methodCandidates the candidates to sort
     * @return a list of size>1 when also candidate 1 is accessible... this will result in an error?
     */
    private List<MethodTypeParameterMap> sortRemainingCandidatesByShallowPublic(Map<MethodTypeParameterMap, Integer> methodCandidates) {
        if (methodCandidates.size() > 1) {
            Comparator<MethodTypeParameterMap> comparator =
                    (m1, m2) -> {
                        boolean m1Accessible = m1.methodInfo().hasBeenAnalyzed();
                        boolean m2Accessible = m2.methodInfo().hasBeenAnalyzed();
                        if (m1Accessible && !m2Accessible) return -1;
                        if (m2Accessible && !m1Accessible) return 1;
                        return 0; // don't know what to prioritize
                    };
            List<MethodTypeParameterMap> sorted = new ArrayList<>(methodCandidates.keySet());
            sorted.sort(comparator);
            MethodTypeParameterMap m1 = sorted.get(1);
            if (m1.methodInfo().hasBeenAnalyzed()) {
                return sorted;
            }
            return List.of(sorted.get(0));
        }
        // not two accessible
        return List.copyOf(methodCandidates.keySet());
    }

    /**
     * Build the correct ForwardReturnTypeInfo to properly evaluate the argument at position i
     *
     * @param method         the method candidate that won the selection, so that the formal parameter type can be determined
     * @param i              the position of the argument
     * @param outsideContext contextual information to be merged with the formal parameter type
     * @param extra          information about type parameters gathered earlier
     * @return the contextual information merged with the formal parameter type info, so that evaluation can start
     */
    private ForwardType determineForwardReturnTypeInfo(MethodTypeParameterMap method,
                                                       int i,
                                                       ParameterizedType outsideContext,
                                                       TypeParameterMap extra) {
        Objects.requireNonNull(method);
        ParameterizedType parameterType = method.getConcreteTypeOfParameter(runtime, i);
        if (outsideContext == null || outsideContext.isVoid() || outsideContext.typeInfo() == null) {
            // Cannot do better than parameter type, have no outside context;
            ParameterizedType translated = parameterType.applyTranslation(runtime, extra.map());
            return new ForwardTypeImpl(translated, false, extra);
        }
        Set<TypeParameter> typeParameters = parameterType.extractTypeParameters();
        Map<NamedType, ParameterizedType> outsideMap = outsideContext.initialTypeParameterMap(runtime);
        if (typeParameters.isEmpty() || outsideMap.isEmpty()) {
            if (outsideMap.isEmpty()) {
                /* here we test whether the return type of the method is a method type parameter. If so,
                   we have and outside type that we can assign to it. See MethodCall_68, assigning B to type parameter T
                 */
                ParameterizedType returnType = method.getConcreteReturnType(runtime);
                if (returnType.typeParameter() != null) {
                    Map<NamedType, ParameterizedType> translate = Map.of(returnType.typeParameter(), outsideContext);
                    ParameterizedType translated = parameterType.applyTranslation(runtime, translate);
                    return new ForwardTypeImpl(translated, false, extra);
                }
            }
            // No type parameters to fill in or to extract
            ParameterizedType translated = parameterType.applyTranslation(runtime, extra.map());
            return new ForwardTypeImpl(translated, false, extra);
        }
        Map<NamedType, ParameterizedType> translate = new HashMap<>(extra.map());
        for (TypeParameter typeParameter : typeParameters) {
            // can we match? if both are functional interfaces, we know exactly which parameter to match

            // otherwise, we're in a bit of a bind -- they need not necessarily agree
            // List.of(E) --> return is List<E>
            ParameterizedType inMap = outsideMap.get(typeParameter);
            if (inMap != null) {
                translate.put(typeParameter, inMap);
            } else if (typeParameter.isMethodTypeParameter()) {
                // return type is List<E> where E is the method type param; need to match to the type's type param
                TypeParameter typeTypeParameter = tryToFindTypeTypeParameter(method, typeParameter);
                if (typeTypeParameter != null) {
                    ParameterizedType inMap2 = outsideMap.get(typeTypeParameter);
                    if (inMap2 != null) {
                        translate.put(typeParameter, inMap2);
                    }
                }
            }
        }
        if (translate.isEmpty()) {
            // Nothing to translate
            return new ForwardTypeImpl(parameterType, false, extra);
        }
        ParameterizedType translated = parameterType.applyTranslation(runtime, translate);
        // Translated context and parameter
        return new ForwardTypeImpl(translated, false, extra);

    }


    private TypeParameter tryToFindTypeTypeParameter(MethodTypeParameterMap method,
                                                     TypeParameter methodTypeParameter) {
        ParameterizedType formalReturnType = method.getConcreteReturnType(runtime);
        Map<NamedType, ParameterizedType> map = formalReturnType.initialTypeParameterMap(runtime);
        // map points from E as 0 in List to E as 0 in List.of()
        return map.entrySet().stream().filter(e -> methodTypeParameter.equals(e.getValue().typeParameter()))
                .map(e -> (TypeParameter) e.getKey()).findFirst().orElse(null);
    }

    private void trimMethodsWithBestScore(Map<MethodTypeParameterMap, Integer> methodCandidates,
                                          Map<MethodInfo, Integer> compatibilityScore) {
        int min = methodCandidates.keySet().stream()
                .mapToInt(mc -> compatibilityScore.getOrDefault(mc.methodInfo(), 0))
                .min().orElseThrow();
        if (min == notAssignable) throw new UnsupportedOperationException();
        methodCandidates.keySet().removeIf(e ->
                compatibilityScore.getOrDefault(e.methodInfo(), 0) > min);
    }

    // remove varargs if there's also non-varargs solutions
    //
    // this step if AFTER the score step, so we've already dealt with type conversions.
    // we still have to deal with overloads in supertypes, methods with the same type signature
    private static void trimVarargsVsMethodsWithFewerParameters(Map<MethodTypeParameterMap, Integer> methodCandidates) {
        int countVarargs = (int) methodCandidates.keySet().stream().filter(e -> e.methodInfo().isVarargs()).count();
        if (countVarargs > 0 && countVarargs < methodCandidates.size()) {
            methodCandidates.keySet().removeIf(e -> e.methodInfo().isVarargs());
        }
    }

    private void filterCandidatesByParameter(Expression expression,
                                             int pos,
                                             Map<MethodTypeParameterMap, Integer> methodCandidates,
                                             Map<MethodInfo, Integer> compatibilityScore) {
        methodCandidates.keySet().removeIf(mc -> {
            int score = compatibleParameter(expression, pos, mc.methodInfo());
            if (score >= 0) {
                Integer inMap = compatibilityScore.get(mc.methodInfo());
                inMap = inMap == null ? score : score + inMap;
                compatibilityScore.put(mc.methodInfo(), inMap);
            }
            return score < 0;
        });
    }

    // different situations with varargs: method(int p1, String... args)
    // 1: method(1) is possible, but pos will not get here, so there's no reason for incompatibility
    // 2: pos == params.size()-1: method(p, "abc")
    // 3: pos == params.size()-1: method(p, new String[] { "a", "b"} )
    // 4: pos >= params.size(): method(p, "a", "b")  -> we need the base type
    private int compatibleParameter(Expression expression, int pos, MethodInfo methodInspection) {
        assert !expression.isEmpty() : "Should we return NOT_ASSIGNABLE?";
        List<ParameterInfo> params = methodInspection.parameters();

        if (pos >= params.size()) {
            ParameterInfo lastParameter = params.get(params.size() - 1);
            assert lastParameter.isVarArgs();
            ParameterizedType typeOfParameter = lastParameter.parameterizedType().copyWithOneFewerArrays();
            return compatibleParameter(expression, typeOfParameter);
        }
        ParameterInfo parameterInfo = params.get(pos);
        if (pos == params.size() - 1 && parameterInfo.isVarArgs()) {
            int withArrays = compatibleParameter(expression, parameterInfo.parameterizedType());
            int withoutArrays = compatibleParameter(expression, parameterInfo.parameterizedType().copyWithOneFewerArrays());
            if (withArrays == -1) return withoutArrays;
            if (withoutArrays == -1) return withArrays;
            return Math.min(withArrays, withoutArrays);
        }
        // the normal situation
        return compatibleParameter(expression, parameterInfo.parameterizedType());
    }

    private int compatibleParameter(Expression evaluatedExpression, ParameterizedType typeOfParameter) {
        Set<ParameterizedType> erasureTypes = erasureTypes(evaluatedExpression);
        if (!erasureTypes.isEmpty()) {
            return erasureTypes.stream().mapToInt(type -> callIsAssignableFrom(type, typeOfParameter, false))
                    .reduce(notAssignable, (v0, v1) -> {
                        if (v0 < 0) return v1;
                        if (v1 < 0) return v0;
                        return Math.min(v0, v1);
                    });
        }

        /* If the evaluated expression is a method with type parameters, then these type parameters
         are allowed in a reverse way (expect List<String>, accept List<T> with T a type parameter of the method,
         as long as T <- String).
        */
        return callIsAssignableFrom(evaluatedExpression.parameterizedType(), typeOfParameter, false);
    }

    private int callIsAssignableFrom(ParameterizedType actualType, ParameterizedType typeOfParameter, boolean explain) {
        int value = runtime.isAssignableFromCovariantErasure(typeOfParameter, actualType);
        if (explain) {
            LOGGER.error("{} <- {} = {}", typeOfParameter, actualType, value);
        }
        return value;
    }

    private static Set<ParameterizedType> erasureTypes(Expression start) {
        Set<ParameterizedType> set = new HashSet<>();
        start.visit(e -> {
            if (e instanceof ErasedExpression erasedExpression) {
                set.addAll(erasedExpression.erasureTypes());
            }
            return true;
        });
        return Set.copyOf(set);
    }


    @Override
    public GenericsHelper genericsHelper() {
        return genericsHelper;
    }

    @Override
    public HierarchyHelper hierarchyHelper() {
        return hierarchyHelper;
    }
}
