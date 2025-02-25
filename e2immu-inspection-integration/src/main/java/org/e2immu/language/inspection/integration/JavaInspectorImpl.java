package org.e2immu.language.inspection.integration;

import org.e2immu.bytecode.java.asm.ByteCodeInspectorImpl;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.info.ImportComputer;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.Formatter;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.info.ImportComputerImpl;
import org.e2immu.language.cst.impl.info.TypePrinter;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.e2immu.language.cst.print.formatter2.Formatter2Impl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.Context;
import org.e2immu.language.inspection.api.parser.Resolver;
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

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
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
    private List<URI> sourceURIs;
    private List<URI> testURIs;
    private final SourceTypeMapImpl sourceTypeMap = new SourceTypeMapImpl();
    private CompiledTypesManager compiledTypesManager;

    private static boolean setStreamHandlerFactory;
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
    public static final String TEST_PROTOCOL = "testprotocol";
    public static final String TEST_PROTOCOL_PREFIX = TEST_PROTOCOL + ":";

    @Override
    public void initialize(InputConfiguration inputConfiguration) throws IOException {
        List<String> classPathAsList = classPathAsList(inputConfiguration);
        LOGGER.info("Combined classpath and test classpath has {} entries", classPathAsList.size());
        if (inputConfiguration.infoLogClasspath()) {
            for (String cp : classPathAsList) {
                LOGGER.info("Classpath entry: {}", cp);
            }
        }
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

        sourceURIs = computeSourceURIs(sourcePath,
                inputConfiguration.restrictSourceToPackages(), "source path");
        testURIs = computeSourceURIs(testSourcePath,
                inputConfiguration.restrictTestSourceToPackages(), "test source path");
    }

    private static List<String> classPathAsList(InputConfiguration inputConfiguration) {
        Stream<String> compileCp = inputConfiguration.classPathParts().stream();
        Stream<String> runtimeCp = inputConfiguration.runtimeClassPathParts().stream();
        Stream<String> testCompileCp = inputConfiguration.testClassPathParts().stream();
        Stream<String> testRuntimeCp = inputConfiguration.testClassPathParts().stream();
        return Stream.concat(Stream.concat(compileCp, runtimeCp), Stream.concat(testCompileCp, testRuntimeCp))
                .distinct()
                .filter(s -> inputConfiguration.excludeFromClasspath().stream().noneMatch(s::contains))
                .toList();
    }

    private List<URI> computeSourceURIs(Resources sourcePath, List<String> restrictions, String what) {
        List<URI> uris = new LinkedList<>();
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
                        uris.add(uri);
                        parts[n] = typeName;
                    } else {
                        ignored.incrementAndGet();
                    }
                }
            }
        });
        LOGGER.info("Found {} .java files in {}, skipped {}", uris.size(), what, ignored);
        return List.copyOf(uris);
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
            } else if (part.startsWith(TEST_PROTOCOL_PREFIX)) {
                try {
                    resources.addTestProtocol(new URI(part));
                } catch (URISyntaxException e) {
                    LOGGER.error("Illegal test protocol {}", part);
                }
            } else {
                File directory = new File(part);
                if (directory.isDirectory()) {
                    String what = isClassPath ? "classpath" : "source path";
                    LOGGER.info("Adding {} to " + what, directory.getAbsolutePath());
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
    public List<TypeInfo> parseReturnAll(String input, boolean detailedSources) {
        Summary failFastSummary = new SummaryImpl(true);
        try {
            URI uri = new URI("input");
            return internalParse(failFastSummary, uri, () -> {
                JavaParser parser = new JavaParser(input);
                parser.setParserTolerant(false);
                return parser;
            }, detailedSources);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private List<TypeInfo> internalParse(Summary summary, URI uri, Supplier<JavaParser> parser, boolean detailedSources) {
        Resolver resolver = new ResolverImpl(runtime.computeMethodOverrides(), new ParseHelperImpl(runtime));
        TypeContextImpl typeContext = new TypeContextImpl(compiledTypesManager, sourceTypeMap);
        Context rootContext = ContextImpl.create(runtime, summary, resolver, typeContext, detailedSources);
        ScanCompilationUnit scanCompilationUnit = new ScanCompilationUnit(summary, runtime);

        ScanCompilationUnit.ScanResult sr = scanCompilationUnit.scan(uri, parser.get().CompilationUnit());
        sourceTypeMap.putAll(sr.sourceTypes());
        CompilationUnit cu = sr.compilationUnit();

        ParseCompilationUnit parseCompilationUnit = new ParseCompilationUnit(rootContext);
        List<TypeInfo> types = parseCompilationUnit.parse(cu, parser.get().CompilationUnit());
        rootContext.resolver().resolve();
        return types;
    }

    @Override
    public Summary parse(URI uri, boolean detailedSources) {
        Summary summary = new SummaryImpl(true); // once stable, change to false

        try (InputStreamReader isr = makeInputStreamReader(uri); StringWriter sw = new StringWriter()) {
            isr.transferTo(sw);
            String sourceCode = sw.toString();

            internalParse(summary, uri, () -> {
                JavaParser parser = new JavaParser(sourceCode);
                parser.setParserTolerant(false);
                return parser;
            }, detailedSources);
        } catch (IOException io) {
            LOGGER.error("Caught IO exception", io);

            summary.addParserError(io);
        }
        return summary;
    }

    private record URICompilationUnit(URI uri, org.parsers.java.ast.CompilationUnit cu, CompilationUnit parsedCu) {
    }

    private InputStreamReader makeInputStreamReader(URI uri) throws IOException {
        return new InputStreamReader(uri.toURL().openStream(), StandardCharsets.UTF_8);
    }

    @Override
    public Summary parse(boolean failFast, boolean detailedSources, Map<String, String> sourcesByTestProtocolURIString) {
        Summary summary = new SummaryImpl(true); // once stable, change to false
        Resolver resolver = new ResolverImpl(runtime.computeMethodOverrides(), new ParseHelperImpl(runtime));
        TypeContextImpl typeContext = new TypeContextImpl(compiledTypesManager, sourceTypeMap);
        Context rootContext = ContextImpl.create(runtime, summary, resolver, typeContext, detailedSources);

        // PHASE 1: scanning all the types, call CongoCC parser

        List<URI> allURIs = Stream.concat(sourceURIs.stream(), testURIs.stream()).toList();

        List<URICompilationUnit> list = new ArrayList<>(allURIs.size());
        for (URI uri : allURIs) {
            String uriString = uri.toString();
            if (uriString.startsWith(TEST_PROTOCOL_PREFIX)) {
                String source = sourcesByTestProtocolURIString.get(uriString);
                parseSourceString(uri, source, summary, list);
            } else {
                try (InputStreamReader isr = makeInputStreamReader(uri); StringWriter sw = new StringWriter()) {
                    isr.transferTo(sw);
                    String sourceCode = sw.toString();

                    parseSourceString(uri, sourceCode, summary, list);
                } catch (IOException io) {
                    LOGGER.error("Caught IO exception", io);
                    summary.addParserError(io);
                }
            }
        }

        // PHASE 2: actual parsing of types, methods, fields

        for (URICompilationUnit uc : list) {
            ParseCompilationUnit parseCompilationUnit = new ParseCompilationUnit(rootContext);
            LOGGER.debug("Parsing {}", uc.uri);
            List<TypeInfo> types = parseCompilationUnit.parse(uc.parsedCu, uc.cu);
            types.forEach(ti -> summary.addType(ti, true));
        }

        // PHASE 3: resolving: content of methods, field initializers

        rootContext.resolver().resolve();
        return summary;
    }

    private void parseSourceString(URI uri, String sourceCode, Summary summary, List<URICompilationUnit> list) {
        JavaParser parser = new JavaParser(sourceCode);
        parser.setParserTolerant(false);

        ScanCompilationUnit scanCompilationUnit = new ScanCompilationUnit(summary, runtime);
        org.parsers.java.ast.CompilationUnit cu = parser.CompilationUnit();
        LOGGER.debug("Scanning {}", uri);
        ScanCompilationUnit.ScanResult sr = scanCompilationUnit.scan(uri, cu);
        sourceTypeMap.putAll(sr.sourceTypes());
        CompilationUnit compilationUnit = sr.compilationUnit();
        URICompilationUnit uc = new URICompilationUnit(uri, cu, compilationUnit);
        list.add(uc);
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
    public List<URI> sourceURIs() {
        return sourceURIs;
    }

    @Override
    public List<URI> testURIs() {
        return testURIs;
    }

    @Override
    public ImportComputer importComputer(int i) {
        return new ImportComputerImpl(i, packageName -> {
            List<TypeInfo> st = sourceTypeMap.primaryTypesInPackage(packageName);
            if (!st.isEmpty()) return st;
            return compiledTypesManager.primaryTypesInPackage(packageName);
        });
    }

    @Override
    public String print2(TypeInfo typeInfo) {
        OutputBuilder ob = new TypePrinter(typeInfo).print(importComputer(4),
                runtime.qualificationQualifyFromPrimaryType(null), true);
        Formatter formatter = new Formatter2Impl(runtime, new FormattingOptionsImpl.Builder().build());
        return formatter.write(ob);
    }
}

