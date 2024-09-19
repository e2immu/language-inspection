package org.e2immu.language.inspection.impl.parser;

import org.e2immu.annotation.NotNull;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.ImportStatement;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.inspection.api.parser.StaticImportMap;
import org.e2immu.language.inspection.api.parser.SourceTypeMap;
import org.e2immu.language.inspection.api.parser.TypeContext;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.Resources;
import org.e2immu.language.inspection.api.resource.SourceFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class TypeContextImpl implements TypeContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(TypeContextImpl.class);

    private record Data(CompiledTypesManager compiledTypesManager,
                        SourceTypeMap sourceTypeMap,
                        StaticImportMap staticImportMap,
                        CompilationUnit compilationUnit) {
        Data withCompilationUnit(CompilationUnit cu) {
            return new Data(compiledTypesManager, sourceTypeMap, new StaticImportMapImpl(), cu);
        }
    }

    private final TypeContextImpl parentContext;
    private final Data data;
    private final Map<String, NamedType> map = new HashMap<>();

    /*
    the packageInfo should already contain all the types of the current package
     */
    public TypeContextImpl(CompiledTypesManager compiledTypesManager, SourceTypeMap sourceTypeMap) {
        this(null, new Data(compiledTypesManager, sourceTypeMap, null, null));
    }

    private TypeContextImpl(TypeContextImpl parentContext, Data data) {
        this.parentContext = parentContext;
        this.data = data;
    }

    @Override
    public void addToStaticImportMap(ImportStatement importStatement) {
        assert importStatement.isStatic();

        String fqnWithAsterisk = importStatement.importString();
        boolean isAsterisk = fqnWithAsterisk.endsWith(".*");
        String fqn = isAsterisk ? fqnWithAsterisk.substring(0, fqnWithAsterisk.length() - 2) : fqnWithAsterisk;

        if (isAsterisk) {
            TypeInfo typeInfo = loadTypeDoNotImport(fqn);
            LOGGER.debug("Add import static wildcard {}", typeInfo);
            addImportStaticWildcard(typeInfo);
        } else {
            int dot = fqn.lastIndexOf('.');
            String typeOrSubTypeName = fqn.substring(0, dot);
            String member = fqn.substring(dot + 1);
            TypeInfo typeInfo = loadTypeDoNotImport(typeOrSubTypeName);
            LOGGER.debug("Add import static, type {}, member {}", typeInfo, member);
            addImportStatic(typeInfo, member);
        }
    }

    @Override
    public void addNonStaticImportToContext(ImportStatement importStatement) {
        assert !importStatement.isStatic();
        String fqnWithAsterisk = importStatement.importString();
        boolean isAsterisk = fqnWithAsterisk.endsWith(".*");
        if (isAsterisk) {
            String fullyQualified = fqnWithAsterisk.substring(0, fqnWithAsterisk.length() - 2);

            LOGGER.debug("Need to parse package {}", fullyQualified);
            String packageName = data.compilationUnit.packageName();
            if (!fullyQualified.equals(packageName)) { // would be our own package; they are already there
                // we either have a type, a subtype, or a package
                TypeInfo inSourceTypes = data.sourceTypeMap.get(fullyQualified);
                if (inSourceTypes == null) {
                    // deal with package
                    for (TypeInfo typeInfo : data.sourceTypeMap.primaryTypesInPackage(fullyQualified)) {
                        if (typeInfo.fullyQualifiedName().equals(fullyQualified + "." + typeInfo.simpleName())) {
                            addToContext(typeInfo, false);
                        }
                    }
                } else {
                    // we must import all subtypes
                    inSourceTypes.subTypes().forEach(st -> map.putIfAbsent(st.simpleName(), st));
                }
                // TODO this should be java-specific (call to data.compiledTypesManager.XXX)
                data.compiledTypesManager.classPath().expandLeaves(fullyQualified, ".class", (expansion, urls) -> {
                    String leaf = expansion[expansion.length - 1];
                    if (!leaf.contains("$")) {
                        // primary type
                        String simpleName = Resources.stripDotClass(leaf);
                        URI uri = urls.get(0);
                        String path = fullyQualified.replace(".", "/") + "/" + simpleName + ".class";
                        TypeInfo newTypeInfo = data.compiledTypesManager.load(new SourceFile(path, uri));
                        if (newTypeInfo != null) {
                            LOGGER.debug("Registering inspection handler for {}", newTypeInfo);
                            addToContext(newTypeInfo, false);
                        } else {
                            LOGGER.error("Could not load {}, URI {}", path, uri);
                        }
                    }
                });
            }
        } else {
            TypeInfo inSourceTypes = data.sourceTypeMap.get(importStatement.importString());
            if (inSourceTypes == null) {
                TypeInfo inCompiledTypes = data.compiledTypesManager.getOrLoad(importStatement.importString());
                if (inCompiledTypes != null) {
                    addToContext(inCompiledTypes, true);
                } else {
                    LOGGER.error("Cannot handle import {}", importStatement.importString());
                }
            } else {
                addToContext(inSourceTypes, true);
            }
        }
    }


    private TypeInfo loadTypeDoNotImport(String fqn) {
        TypeInfo inSourceTypes = data.sourceTypeMap.get(fqn);
        if (inSourceTypes != null) {
            return inSourceTypes;
        }
        TypeInfo compiled = data.compiledTypesManager.get(fqn);
        if (compiled != null) {
            if (!compiled.hasBeenInspected()) {
                data.compiledTypesManager.ensureInspection(compiled);
            }
            return compiled;
        }
        SourceFile path = data.compiledTypesManager.fqnToPath(fqn, ".class");
        if (path == null) {
            LOGGER.error("ERROR: Cannot find type '{}'", fqn);
            throw new UnsupportedOperationException(fqn);
        }
        return data.compiledTypesManager.load(path);
    }

    @Override
    public TypeContext newCompilationUnit(CompilationUnit compilationUnit) {
        return new TypeContextImpl(null, data.withCompilationUnit(compilationUnit));
    }

    @Override
    public StaticImportMap importMap() {
        return data.staticImportMap;
    }

    @Override
    public CompilationUnit compilationUnit() {
        return data.compilationUnit;
    }

    /**
     * Look up a type by FQN. Ensure that the type has been inspected.
     *
     * @param fullyQualifiedName the fully qualified name, such as java.lang.String
     * @return the type
     */
    private TypeInfo getFullyQualified(String fullyQualifiedName) {
        TypeInfo sourceType = data.sourceTypeMap.get(fullyQualifiedName);
        if (sourceType != null) {
            return sourceType;
        }
        TypeInfo typeInfo = data.compiledTypesManager.getOrLoad(fullyQualifiedName);
        if (typeInfo != null) {
            data.compiledTypesManager.ensureInspection(typeInfo);
        }
        return typeInfo;
    }

    @Override
    public NamedType get(String name, boolean complain) {
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            NamedType simple = getSimpleName(name);
            if (simple != null) {
                return simple;
            }
        }
        // name can be fully qualified, or semi qualified; but the package can be empty, too.
        // try fully qualified first
        NamedType fullyQualified = getFullyQualified(name);
        if (fullyQualified != null) return fullyQualified;

        if (dot >= 0) {
            // it must be semi qualified now... go recursive;
            String prefix = name.substring(0, dot);
            NamedType prefixType = get(prefix, complain);
            if (prefixType instanceof TypeInfo typeInfo) {
                String tail = name.substring(dot + 1);
                TypeInfo tailType = subTypeOfRelated(typeInfo, tail);
                if (tailType != null) {
                    return tailType;
                }
            }
        }

        NamedType javaLang = data.compiledTypesManager.get("java.lang." + name);
        if (complain && javaLang == null) {
            throw new UnsupportedOperationException("Cannot find type " + name);
        }
        return javaLang;
    }

    private TypeInfo subTypeOfRelated(TypeInfo typeInfo, String name) {
        TypeInfo sub = typeInfo.findSubType(name, false);
        if (sub != null) return sub;
        if (typeInfo.parentClass() != null && !typeInfo.parentClass().typeInfo().isJavaLangObject()) {
            TypeInfo subParent = subTypeOfRelated(typeInfo.parentClass().typeInfo(), name);
            if (subParent != null) return subParent;
        }
        for (ParameterizedType interfaceImplemented : typeInfo.interfacesImplemented()) {
            TypeInfo subImplemented = subTypeOfRelated(interfaceImplemented.typeInfo(), name);
            if (subImplemented != null) {
                return subImplemented;
            }
        }
        return null;
    }


    private NamedType getSimpleName(String name) {
        NamedType namedType = map.get(name);
        if (namedType != null) {
            return namedType;
        }

        // Same package, and * imports (in that order!)
        if (parentContext != null) {
            NamedType fromParent = parentContext.getSimpleName(name);
            if (fromParent != null) {
                return fromParent;
            }
        }

        /*
        On-demand: subtype from import static statement (see e.g. Import_2)
        This is done on-demand to fight cyclic dependencies if we do eager inspection.
         */
        TypeInfo parent = data.staticImportMap.getStaticMemberToTypeInfo(name);
        if (parent != null) {
            TypeInfo subType = parent.subTypes()
                    .stream().filter(st -> name.equals(st.simpleName())).findFirst().orElse(null);
            if (subType != null) {
                map.put(name, subType);
                return subType;
            }
        }
        return null;
    }

    @Override
    public void addToContext(@NotNull NamedType namedType) {
        addToContext(namedType, true);
    }

    @Override
    public void addToContext(@NotNull NamedType namedType, boolean allowOverwrite) {
        String simpleName = namedType.simpleName();
        if (allowOverwrite || !map.containsKey(simpleName)) {
            map.put(simpleName, namedType);
        }
    }

    @Override
    public void addToContext(String altName, @NotNull NamedType namedType, boolean allowOverwrite) {
        if (allowOverwrite || !map.containsKey(altName)) {
            map.put(altName, namedType);
        }
    }

    @Override
    public TypeContext newTypeContext() {
        return new TypeContextImpl(this, data);
    }

    public void addImportStaticWildcard(TypeInfo typeInfo) {
        data.staticImportMap.addStaticAsterisk(typeInfo);
    }

    public void addImportStatic(TypeInfo typeInfo, String member) {
        data.staticImportMap.putStaticMemberToTypeInfo(member, typeInfo);
    }

    @Override
    public Map<String, FieldReference> staticFieldImports(Runtime runtime) {
        Map<String, FieldReference> map = new HashMap<>();
        for (Map.Entry<String, TypeInfo> entry : data.staticImportMap.staticMemberToTypeInfoEntrySet()) {
            TypeInfo typeInfo = entry.getValue();
            String memberName = entry.getKey();
            typeInfo.fields().stream()
                    .filter(FieldInfo::isStatic)
                    .filter(f -> f.name().equals(memberName))
                    .findFirst()
                    .ifPresent(fieldInfo -> map.put(memberName, runtime.newFieldReference(fieldInfo)));
        }
        for (TypeInfo typeInfo : data.staticImportMap.staticAsterisk()) {
            typeInfo.fields().stream()
                    .filter(FieldInfo::isStatic)
                    .forEach(fieldInfo -> map.put(fieldInfo.name(), runtime.newFieldReference(fieldInfo)));
        }
        return map;
    }

    @Override
    public TypeContext newAnonymousClassBody(TypeInfo baseType) {
        addSubTypesOfHierarchy(baseType);
        return new TypeContextImpl(this, data);
    }

    @Override
    public List<TypeInfo> typesInSamePackage(String packageName) {
        return data.sourceTypeMap().primaryTypesInPackage(packageName);
    }

    @Override
    public void addSubTypesOfHierarchy(TypeInfo typeInfo) {
        CompilationUnit cu = typeInfo.compilationUnit();
        Stream.concat(Stream.of(typeInfo), typeInfo.superTypesExcludingJavaLangObject().stream())
                .forEach(superType -> superType.subTypes()
                        .stream().filter(st -> st.compilationUnit() == cu || !typeInfo.access().isPrivate())
                        .forEach(this::addToContext));
    }
}
