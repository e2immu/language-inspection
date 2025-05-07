package org.e2immu.language.inspection.impl.parser;

import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.api.parser.ParseResult;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ParseResultImpl implements ParseResult {
    private static final Set<TypeInfo> NO_CHILDREN = Set.of();
    private final Set<TypeInfo> types;
    private final Map<String, TypeInfo> typesByFQN;
    private final Map<String, Set<TypeInfo>> primaryTypesOfPackage;
    private final Map<TypeInfo, Set<TypeInfo>> children;

    public ParseResultImpl(Set<TypeInfo> types) {
        this.types = types;
        typesByFQN = types.stream()
                .flatMap(TypeInfo::recursiveSubTypeStream)
                .collect(Collectors.toUnmodifiableMap(Info::fullyQualifiedName, t -> t));
        Map<String, Set<TypeInfo>> mutableTypesOfPackage = new HashMap<>();
        types.forEach(ti -> mutableTypesOfPackage.computeIfAbsent(ti.packageName(),
                t -> new HashSet<>()).add(ti));
        mutableTypesOfPackage.replaceAll((k, s) -> Set.copyOf(s));
        primaryTypesOfPackage = Map.copyOf(mutableTypesOfPackage);
        Map<TypeInfo, Set<TypeInfo>> children = new HashMap<>();
        types.stream().flatMap(TypeInfo::recursiveSubTypeStream).forEach(t -> {
            if (t.parentClass() != null && !t.parentClass().typeInfo().isJavaLangObject()) {
                children.computeIfAbsent(t.parentClass().typeInfo(), type -> new HashSet<>()).add(t);
            }
            for (ParameterizedType pt : t.interfacesImplemented()) {
                children.computeIfAbsent(pt.typeInfo(), type -> new HashSet<>()).add(t);
            }
        });
        this.children = Map.copyOf(children);
    }

    @Override
    public Set<TypeInfo> descendants(TypeInfo typeInfo, boolean recurse) {
        if (recurse) {
            Set<TypeInfo> all = new HashSet<>();
            recursivelyComputeDescendants(typeInfo, all, new HashSet<>());
            return all;
        }
        return this.children.getOrDefault(typeInfo, NO_CHILDREN);
    }

    private void recursivelyComputeDescendants(TypeInfo typeInfo, Set<TypeInfo> all, Set<Object> seen) {
        if (seen.add(typeInfo)) {
            Set<TypeInfo> descendants = children.getOrDefault(typeInfo, NO_CHILDREN);
            all.addAll(descendants);
            for (TypeInfo child : descendants) {
                recursivelyComputeDescendants(child, all, seen);
            }
        }
    }

    @Override
    public Set<TypeInfo> primaryTypes() {
        return types;
    }

    @Override
    public TypeInfo firstType() {
        return types.stream().findFirst().orElseThrow();
    }

    @Override
    public TypeInfo findType(String fqn) {
        return typesByFQN.get(fqn);
    }

    @Override
    public Set<TypeInfo> primaryTypesOfPackage(String packageName) {
        return primaryTypesOfPackage.get(packageName);
    }

    @Override
    public int size() {
        return types.size();
    }

    private static final Pattern METHOD = Pattern.compile("([^(]+)\\.[^(.]+\\(.+");

    @Override
    public MethodInfo findMethod(String methodFqn, boolean complain) {
        Matcher m = METHOD.matcher(methodFqn);
        if (m.matches()) {
            String typeFqn = m.group(1);
            TypeInfo typeInfo = findType(typeFqn);
            if (typeInfo == null) {
                if (complain) {
                    throw new RuntimeException("Cannot find type with fqn '" + typeFqn + "'");
                }
                return null;
            }
            MethodInfo methodInfo = typeInfo.constructorAndMethodStream()
                    .filter(mi -> mi.fullyQualifiedName().equals(methodFqn))
                    .findFirst().orElse(null);
            if (methodInfo != null) return methodInfo;
        }
        if (complain) {
            throw new RuntimeException("Cannot find method with fqn '" + methodFqn + "'");
        }
        return null;
    }

    @Override
    public Map<String, Set<TypeInfo>> primaryTypesPerPackage() {
        return primaryTypesOfPackage;
    }
}
