package org.e2immu.language.inspection.integration;

import org.e2immu.bytecode.java.asm.ByteCodeInspectorImpl;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.Context;
import org.e2immu.language.inspection.api.parser.Resolver;
import org.e2immu.language.inspection.api.parser.SourceTypes;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.resource.*;
import org.e2immu.language.inspection.impl.parser.*;
import org.e2immu.language.inspection.resource.CompiledTypesManagerImpl;
import org.e2immu.language.inspection.resource.ResourcesImpl;
import org.e2immu.parser.java.ParseCompilationUnit;
import org.e2immu.parser.java.ParseHelperImpl;
import org.e2immu.parser.java.ScanCompilationUnit;
import org.parsers.java.JavaParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
from input configuration
to classpath + sourceTypeMap/Trie

then, do a 1st round of parsing the source types -> map fqn->type/subType + list compilation unit->parsed object

finally, do the actual parsing for all primary types
 */
public class JavaInspectorImpl implements JavaInspector {
    private Runtime runtime;
    private final SourceTypes sourceTypes = new SourceTypesImpl();
    private final SourceTypeMapImpl sourceTypeMap = new SourceTypeMapImpl();
    private CompiledTypesManager compiledTypesManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaInspector.class);

    /**
     * Use of this prefix in parts of the input classpath allows for adding jars
     * on the current classpath containing the path following the prefix.
     * <p>
     * For example, adding
     * <p>
     * Adds the content of
     * <p>
     * jar:file:/Users/bnaudts/.gradle/caches/modules-2/files-2.1/com.google.guava/guava/28.1-jre/b0e91dcb6a44ffb6221b5027e12a5cb34b841145/guava-28.1-jre.jar!/
     */
    public static final String JAR_WITH_PATH_PREFIX = "jar-on-classpath:";

    @Override
    public void initialize(InputConfiguration inputConfiguration) throws IOException {
        List<String> classPathAsList = classPathAsList(inputConfiguration);
        LOGGER.info("Combined classpath and test classpath has {} entries", classPathAsList.size());
        Resources classPath = assemblePath(inputConfiguration, true, "Classpath", classPathAsList);
        CompiledTypesManagerImpl ctm = new CompiledTypesManagerImpl(classPath);
        runtime = new RuntimeWithCompiledTypesManager(ctm);
        ByteCodeInspector byteCodeInspector = new ByteCodeInspectorImpl(runtime, ctm);
        ctm.setByteCodeInspector(byteCodeInspector);
        this.compiledTypesManager = ctm;

        for (String packageName : new String[]{"org.e2immu.annotation", "java.lang", "java.util.function"}) {
            preload(packageName);
        }

        Resources sourcePath = assemblePath(inputConfiguration, false, "Source path",
                inputConfiguration.sources());
        Resources testSourcePath = assemblePath(inputConfiguration, false, "Test source path",
                inputConfiguration.testSources());

        Map<TypeInfo, URI> sourceURLs = computeSourceURLs(sourcePath,
                inputConfiguration.restrictSourceToPackages(), "source path");
        Map<TypeInfo, URI> testSourceURLs = computeSourceURLs(testSourcePath,
                inputConfiguration.restrictTestSourceToPackages(), "test source path");
        sourceURLs.putAll(testSourceURLs);
        sourceTypes.freeze();
    }


    private static List<String> classPathAsList(InputConfiguration inputConfiguration) {
        Stream<String> compileCp = inputConfiguration.classPathParts().stream();
        Stream<String> runtimeCp = inputConfiguration.runtimeClassPathParts().stream();
        Stream<String> testCompileCp = inputConfiguration.testClassPathParts().stream();
        Stream<String> testRuntimeCp = inputConfiguration.testClassPathParts().stream();
        return Stream.concat(Stream.concat(compileCp, runtimeCp), Stream.concat(testCompileCp, testRuntimeCp))
                .distinct().toList();
    }

    private Map<TypeInfo, URI> computeSourceURLs(Resources sourcePath, List<String> restrictions, String what) {
        Map<TypeInfo, URI> sourceURLs = new HashMap<>();
        AtomicInteger ignored = new AtomicInteger();
        sourcePath.visit(new String[0], (parts, list) -> {
            if (parts.length >= 1) {
                int n = parts.length - 1;
                String name = parts[n];
                if (name.endsWith(".java") && !"package-info.java".equals(name)) {
                    String typeName = name.substring(0, name.length() - 5);
                    String packageName = Arrays.stream(parts).limit(n).collect(Collectors.joining("."));
                    if (acceptSource(packageName, typeName, restrictions)) {
                        URI uri = list.get(0);
                        CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName(packageName)
                                .setURI(uri).build();
                        TypeInfo typeInfo = runtime.newTypeInfo(cu, typeName);
                        sourceURLs.put(typeInfo, uri);
                        parts[n] = typeName;
                        sourceTypes.add(parts, typeInfo);
                    } else {
                        ignored.incrementAndGet();
                    }
                }
            }
        });
        LOGGER.info("Found {} .java files in {}, skipped {}", sourceURLs.size(), what, ignored);
        return sourceURLs;
    }

    public static boolean acceptSource(String packageName, String typeName, List<String> restrictions) {
        if (restrictions.isEmpty()) return true;
        for (String packageString : restrictions) {
            if (packageString.endsWith(".")) {
                if (packageName.startsWith(packageString) ||
                    packageName.equals(packageString.substring(0, packageString.length() - 1))) return true;
            } else if (packageName.equals(packageString) || packageString.equals(packageName + "." + typeName))
                return true;
        }
        return false;
    }

    /**
     * IMPORTANT: this method assumes that the jmod 'java.base.jmod' is on the class path
     * if not, the method will have little effect and no classes beyond the ones from
     * <code>initializeClassPath</code> will be present
     */
    @Override
    public void preload(String thePackage) {
        compiledTypesManager.preload(thePackage);
    }

    @Override
    public void loadByteCodeQueue() {
        compiledTypesManager.loadByteCodeQueue();
    }

    private static Resources assemblePath(InputConfiguration configuration,
                                          boolean isClassPath,
                                          String msg,
                                          List<String> parts) throws IOException {
        Resources resources = new ResourcesImpl();
        if (isClassPath) {
            Map<String, Integer> entriesAdded = resources.addJarFromClassPath("org/e2immu/annotation");
            if (entriesAdded.size() != 1 || entriesAdded.values().stream().findFirst().orElseThrow() < 10) {
                throw new RuntimeException("? expected 1 jar, at least 10 entries; got " + entriesAdded.size());
            }
        }
        for (String part : parts) {
            if (part.startsWith(JAR_WITH_PATH_PREFIX)) {
                Map<String, Integer> entriesAdded = resources.addJarFromClassPath(part.substring(JAR_WITH_PATH_PREFIX.length()));
                LOGGER.debug("Found {} jar(s) on classpath for {}", entriesAdded.size(), part);
                entriesAdded.forEach((p, n) -> LOGGER.debug("  ... added {} entries for jar {}", n, p));
            } else if (part.endsWith(".jar")) {
                try {
                    // "jar:file:build/libs/equivalent.jar!/"
                    URL url = new URL("jar:file:" + part + "!/");
                    int entries = resources.addJar(url);
                    LOGGER.debug("Added {} entries for jar {}", entries, part);
                } catch (IOException e) {
                    LOGGER.error("{} part '{}' ignored: IOException {}", msg, part, e.getMessage());
                }
            } else if (part.endsWith(".jmod")) {
                try {
                    URL url = ResourcesImpl.constructJModURL(part, configuration.alternativeJREDirectory());
                    int entries = resources.addJmod(url);
                    LOGGER.debug("Added {} entries for jmod {}", entries, part);
                } catch (IOException e) {
                    LOGGER.error("{} part '{}' ignored: IOException {}", msg, part, e.getMessage());
                }
            } else {
                File directory = new File(part);
                if (directory.isDirectory()) {
                    LOGGER.info("Adding {} to classpath", directory.getAbsolutePath());
                    resources.addDirectoryFromFileSystem(directory);
                } else {
                    LOGGER.error("{} part '{}' is not a .jar file, and not a directory: ignored", msg, part);
                }
            }
        }
        return resources;
    }

    // used for testing
    @Override
    public TypeInfo parse(String input) {
        return parseReturnAll(input).get(0);
    }

    @Override
    public List<TypeInfo> parseReturnAll(String input) {
        Summary failFastSummary = new SummaryImpl(true);
        return internalParse(failFastSummary, () -> {
            JavaParser parser = new JavaParser(input);
            parser.setParserTolerant(false);
            return parser;
        });
    }

    private List<TypeInfo> internalParse(Summary failFastSummary, Supplier<JavaParser> parser) {
        Resolver resolver = new ResolverImpl(runtime.computeMethodOverrides(), new ParseHelperImpl(runtime));
        TypeContextImpl typeContext = new TypeContextImpl(compiledTypesManager, sourceTypes, sourceTypeMap);
        Context rootContext = ContextImpl.create(runtime, failFastSummary, resolver, typeContext);
        ScanCompilationUnit scanCompilationUnit = new ScanCompilationUnit(rootContext);
        CompilationUnit cu;
        try {
            ScanCompilationUnit.ScanResult sr = scanCompilationUnit.scan(new URI("input"), parser.get().CompilationUnit());
            sourceTypeMap.putAll(sr.sourceTypes());
            cu = sr.compilationUnit();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        ParseCompilationUnit parseCompilationUnit = new ParseCompilationUnit(rootContext);
        List<TypeInfo> types = parseCompilationUnit.parse(cu, parser.get().CompilationUnit());
        rootContext.resolver().resolve();
        return types;
    }

    @Override
    public Summary parse(TypeInfo typeInfo) {
        Summary summary = new SummaryImpl(true); // once stable, change to false
        URI uri = typeInfo.compilationUnit().uri();

        try (InputStreamReader isr = new InputStreamReader(uri.toURL().openStream(), StandardCharsets.UTF_8);
             StringWriter sw = new StringWriter()) {
            isr.transferTo(sw);
            String sourceCode = sw.toString();

            List<TypeInfo> types = internalParse(summary, () -> {
                JavaParser parser = new JavaParser(sourceCode);
                parser.setParserTolerant(false);
                return parser;
            });
            assert types.stream().anyMatch(ti -> ti == typeInfo);
        } catch (IOException io) {
            LOGGER.error("Caught IO exception", io);

            summary.addParserError(io);
        }
        return summary;
    }

    @Override
    public Runtime runtime() {
        return runtime;
    }

    @Override
    public CompiledTypesManager compiledTypesManager() {
        return compiledTypesManager;
    }

    @Override
    public SourceTypes sourceTypes() {
        return sourceTypes;
    }
}

