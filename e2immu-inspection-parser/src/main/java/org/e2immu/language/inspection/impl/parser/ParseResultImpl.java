package org.e2immu.language.inspection.impl.parser;

import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.ParseResult;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ParseResultImpl implements ParseResult {
    private final Set<TypeInfo> types;
    private final Map<String, TypeInfo> typesByFQN;
    private final Map<String, Set<TypeInfo>> primaryTypesOfPackage;

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
    public MethodInfo findMethod(String methodFqn) {
        Matcher m = METHOD.matcher(methodFqn);
        if (m.matches()) {
            String typeFqn = m.group(1);
            TypeInfo typeInfo = findType(typeFqn);
            return typeInfo.methodStream().filter(mi -> mi.fullyQualifiedName().equals(methodFqn))
                    .findFirst().orElseThrow();
        }
        throw new RuntimeException("Cannot find method with fqn '" + methodFqn + "'");
    }

    @Override
    public Map<String, Set<TypeInfo>> primaryTypesPerPackage() {
        return primaryTypesOfPackage;
    }
}
