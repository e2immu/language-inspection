package org.e2immu.language.inspection.integration;

import org.e2immu.bytecode.java.asm.ByteCodeInspectorImpl;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.FingerPrint;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.ImportComputer;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.Formatter;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.runtime.Runtime;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
from input configuration
to classpath + sourceTypeMap/Trie

then, do a 1st round of parsing the source types -> map fqn->type/subType + list compilation unit->parsed object

finally, do the actual parsing for all primary types
 */
public class JavaInspectorImpl implements JavaInspector {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaInspectorImpl.class);

    private Runtime runtime;
    private List<SourceFile> sourceURIs;
    private List<SourceFile> testURIs;
    private final SourceTypeMapImpl sourceTypeMap = new SourceTypeMapImpl();
    private CompiledTypesManager compiledTypesManager;
    private final boolean computeFingerPrints;

    public JavaInspectorImpl() {
        this(false);
    }

    public JavaInspectorImpl(boolean computeFingerPrints) {
        this.computeFingerPrints = computeFingerPrints;
    }

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
    public static final String E2IMMU_SUPPORT = JAR_WITH_PATH_PREFIX + "org/e2immu/annotation";

    public static final String TEST_PROTOCOL_PREFIX = TEST_PROTOCOL + ":";
    public static final ParseOptions FAIL_FAST = new ParseOptions(true, false,
            false);

    public static class ParseOptionsBuilder implements JavaInspector.ParseOptionsBuilder {
        private boolean failFast;
        private boolean detailedSources;
        private boolean allowCreationOfStubTypes;

        @Override
        public JavaInspector.ParseOptionsBuilder setFailFast(boolean failFast) {
            this.failFast = failFast;
            return this;
        }

        @Override
        public JavaInspector.ParseOptionsBuilder setDetailedSources(boolean detailedSources) {
            this.detailedSources = detailedSources;
            return this;
        }

        @Override
        public JavaInspector.ParseOptionsBuilder setAllowCreationOfStubTypes(boolean allowCreationOfStubTypes) {
            this.allowCreationOfStubTypes = allowCreationOfStubTypes;
            return this;
        }

        @Override
        public ParseOptions build() {
            return new ParseOptions(failFast, detailedSources, allowCreationOfStubTypes);
        }
    }

    @Override
    public List<InitializationProblem> initialize(InputConfiguration inputConfiguration) throws IOException {
        List<InitializationProblem> initializationProblems = new LinkedList<>();
        try {
            List<SourceSet> classPathSourceSets = inputConfiguration.classPathParts().stream()
                    .map(set -> (SourceSet) set).toList();
            Resources classPath = assemblePath(inputConfiguration.alternativeJREDirectory(), classPathSourceSets,
                    true, "Classpath", initializationProblems);
            CompiledTypesManagerImpl ctm = new CompiledTypesManagerImpl(classPath);
            runtime = new RuntimeWithCompiledTypesManager(ctm);
            ByteCodeInspector byteCodeInspector = new ByteCodeInspectorImpl(runtime, ctm, computeFingerPrints);
            ctm.setByteCodeInspector(byteCodeInspector);
            this.compiledTypesManager = ctm;

            for (String packageName : new String[]{"java.lang", "java.util.function"}) {
                preload(packageName);
            }

            List<SourceSet> sourcePathSourceSets = inputConfiguration.sourceSets().stream()
                    .filter(set -> !set.test()).toList();
            Resources sourcePath = assemblePath(inputConfiguration.alternativeJREDirectory(),
                    sourcePathSourceSets, false, "Source path", initializationProblems);
            List<SourceSet> testSourcePathSourceSets = inputConfiguration.sourceSets().stream()
                    .filter(SourceSet::test).toList();
            Resources testSourcePath = assemblePath(inputConfiguration.alternativeJREDirectory(), testSourcePathSourceSets,
                    false, "Test source path", initializationProblems);

            sourceURIs = computeSourceURIs(sourcePath, "source path");
            testURIs = computeSourceURIs(testSourcePath, "test source path");
        } catch (URISyntaxException e) {
            LOGGER.error("Caught URISyntaxException, transforming into IOException", e);
            throw new IOException(e);
        }
        return List.copyOf(initializationProblems);
    }

    private List<SourceFile> computeSourceURIs(Resources sourcePath, String what) {
        List<SourceFile> sourceFiles = new LinkedList<>();
        AtomicInteger ignored = new AtomicInteger();
        sourcePath.visit(new String[0], (parts, list) -> {
            if (parts.length >= 1 && !list.isEmpty()) {
                int n = parts.length - 1;
                String name = parts[n];
                if (name.endsWith(".java") && !"package-info.java".equals(name)) {
                    String typeName = name.substring(0, name.length() - 5);
                    String packageName = Arrays.stream(parts).limit(n).collect(Collectors.joining("."));
                    SourceFile sourceFile = list.get(0);
                    if (sourceFile.sourceSet().acceptSource(packageName, typeName)) {
                        sourceFiles.add(sourceFile);
                        parts[n] = typeName;
                    } else {
                        ignored.incrementAndGet();
                    }
                }
            }
        });
        LOGGER.info("Found {} .java files in {}, skipped {}", sourceFiles.size(), what, ignored);
        return List.copyOf(sourceFiles);
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

    private Resources assemblePath(Path alternativeJREDirectory,
                                   List<SourceSet> sourceSets,
                                   boolean isClassPath,
                                   String msg,
                                   List<InitializationProblem> initializationProblems) throws IOException, URISyntaxException {
        Resources resources = new ResourcesImpl();
        for (SourceSet sourceSet : sourceSets) {
            String part = sourceSet.path().toString();
            Throwable throwable = null;
            if (part.startsWith(JAR_WITH_PATH_PREFIX)) {
                String suffix = part.substring(JAR_WITH_PATH_PREFIX.length());
                URL jarUrl = resources.findJarInClassPath(suffix);
                if (jarUrl == null) {
                    String msgString = msg + " part '" + part + "': jar not found";
                    LOGGER.warn(msg);
                    initializationProblems.add(new InitializationProblem(msgString, null));
                } else {
                    addJar(resources, part, jarUrl, sourceSet);
                }
            } else if (part.endsWith(".jar")) {
                try {
                    // "jar:file:build/libs/equivalent.jar!/"
                    URL jarUrl = new URL("jar:file:" + part + "!/");
                    addJar(resources, part, jarUrl, sourceSet);
                } catch (IOException e) {
                    throwable = e;
                }
            } else if (part.endsWith(".jmod")) {
                try {
                    URL url = ResourcesImpl.constructJModURL(part, alternativeJREDirectory);
                    FingerPrint fingerPrint = makeFingerPrint(url);
                    sourceSet.setFingerPrint(fingerPrint);
                    int entries = resources.addJmod(new SourceFile(part, url.toURI(), sourceSet, null));
                    LOGGER.debug("Added {} entries for jmod {}", entries, part);
                } catch (IOException e) {
                    throwable = e;
                }
            } else if (part.startsWith(TEST_PROTOCOL_PREFIX)) {
                try {
                    resources.addTestProtocol(new SourceFile(part, new URI(part), sourceSet, null));
                } catch (URISyntaxException e) {
                    throwable = e;
                }
            } else {
                File directory = new File(part);
                if (directory.isDirectory()) {
                    LOGGER.info("Adding {} to {}", directory.getAbsolutePath(), msg);
                    resources.addDirectoryFromFileSystem(directory, sourceSet);
                } else {
                    String msgString = msg + " part '" + part + "' is not a .jar file, and not a directory: ignored";
                    LOGGER.warn(msgString);
                    initializationProblems.add(new InitializationProblem(msgString, null));
                }
            }
            if (throwable != null) {
                String msgString = msg + " part '" + part + "' ignored: " + throwable.getMessage();
                LOGGER.warn(msgString);
                initializationProblems.add(new InitializationProblem(msgString, throwable));
            }
        }
        return resources;
    }

    private void addJar(Resources resources, String part, URL jarUrl, SourceSet sourceSet) throws IOException, URISyntaxException {
        FingerPrint fingerPrint = makeFingerPrint(jarUrl);
        sourceSet.setFingerPrint(fingerPrint);
        int entries = resources.addJar(new SourceFile(part, jarUrl.toURI(), sourceSet, null));
        LOGGER.debug("Added {} entries for jar {}", entries, part);
    }

    private static final Pattern JAR_URL_PATTERN = Pattern.compile("jar:(file:.+)!/");

    private FingerPrint makeFingerPrint(URL jarUrl) throws URISyntaxException, IOException {
        if (computeFingerPrints) {
            Matcher m = JAR_URL_PATTERN.matcher(jarUrl.toString());
            if (m.matches()) {
                Path path = Path.of(new URI(m.group(1)));
                byte[] bytes = Files.readAllBytes(path);
                return MD5FingerPrint.compute(bytes);
            } else {
                throw new UnsupportedOperationException("? " + jarUrl);
            }
        }
        return MD5FingerPrint.NO_FINGERPRINT;
    }

    // used for testing
    @Override
    public TypeInfo parse(String input, ParseOptions parseOptions) {
        return parseReturnAll(input, parseOptions).get(0);
    }

    @Override
    public TypeInfo parse(String input) {
        return parseReturnAll(input, FAIL_FAST).get(0);
    }

    @Override
    public List<TypeInfo> parseReturnAll(String input, ParseOptions parseOptions) {
        Summary failFastSummary = new SummaryImpl(true);
        try {
            URI uri = new URI("input");
            SourceFile sourceFile = new SourceFile(null, uri, null, MD5FingerPrint.compute(input));
            return internalParse(failFastSummary, sourceFile, () -> {
                JavaParser parser = new JavaParser(input);
                parser.setParserTolerant(false);
                return parser;
            }, parseOptions);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private List<TypeInfo> internalParse(Summary summary,
                                         SourceFile sourceFile,
                                         Supplier<JavaParser> parser,
                                         ParseOptions parseOptions) {
        Resolver resolver = new ResolverImpl(runtime.computeMethodOverrides(), new ParseHelperImpl(runtime));
        TypeContextImpl typeContext = new TypeContextImpl(runtime, compiledTypesManager, sourceTypeMap,
                parseOptions.allowCreationOfStubTypes());
        Context rootContext = ContextImpl.create(runtime, summary, resolver, typeContext,
                parseOptions.detailedSources());
        ScanCompilationUnit scanCompilationUnit = new ScanCompilationUnit(summary, runtime);

        ScanCompilationUnit.ScanResult sr = scanCompilationUnit.scan(sourceFile.uri(), sourceFile.sourceSet(),
                sourceFile.fingerPrint(), parser.get().CompilationUnit(),
                parseOptions.detailedSources());
        sourceTypeMap.putAll(sr.sourceTypes());
        CompilationUnit cu = sr.compilationUnit();

        ParseCompilationUnit parseCompilationUnit = new ParseCompilationUnit(rootContext);
        List<TypeInfo> types = parseCompilationUnit.parse(cu, parser.get().CompilationUnit());
        rootContext.resolver().resolve();
        return types;
    }

    @Override
    public Summary parse(URI uri, ParseOptions parseOptions) {
        Summary summary = new SummaryImpl(true); // once stable, change to false

        try (InputStreamReader isr = makeInputStreamReader(uri); StringWriter sw = new StringWriter()) {
            isr.transferTo(sw);
            String sourceCode = sw.toString();
            SourceFile sourceFile = new SourceFile(uri.toString(), uri, null, MD5FingerPrint.compute(sourceCode));
            internalParse(summary, sourceFile, () -> {
                JavaParser parser = new JavaParser(sourceCode);
                parser.setParserTolerant(false);
                return parser;
            }, parseOptions);
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
    public Summary parse(ParseOptions parseOptions) {
        return parse(Map.of(), parseOptions);
    }

    @Override
    public Summary parse(Map<String, String> sourcesByTestProtocolURIString, ParseOptions parseOptions) {
        Summary summary = new SummaryImpl(true); // once stable, change to false
        Resolver resolver = new ResolverImpl(runtime.computeMethodOverrides(), new ParseHelperImpl(runtime));
        TypeContextImpl typeContext = new TypeContextImpl(runtime, compiledTypesManager, sourceTypeMap,
                parseOptions.allowCreationOfStubTypes());
        Context rootContext = ContextImpl.create(runtime, summary, resolver, typeContext,
                parseOptions.detailedSources());

        // PHASE 1: scanning all the types, call CongoCC parser

        List<SourceFile> allURIs = Stream.concat(sourceURIs.stream(), testURIs.stream()).toList();

        List<URICompilationUnit> list = new ArrayList<>(allURIs.size());
        for (SourceFile sourceFile : allURIs) {
            String uriString = sourceFile.uri().toString();
            if (uriString.startsWith(TEST_PROTOCOL_PREFIX)) {
                String sourceCode = sourcesByTestProtocolURIString.get(uriString);
                parseSourceString(sourceFile.uri(), sourceFile.sourceSet(), sourceCode, summary,
                        list, parseOptions.detailedSources());
            } else {
                try (InputStreamReader isr = makeInputStreamReader(sourceFile.uri()); StringWriter sw = new StringWriter()) {
                    isr.transferTo(sw);
                    String sourceCode = sw.toString();
                    parseSourceString(sourceFile.uri(), sourceFile.sourceSet(), sourceCode, summary, list,
                            parseOptions.detailedSources());
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

    private void parseSourceString(URI uri, SourceSet sourceSet, String sourceCode,
                                   Summary summary, List<URICompilationUnit> list,
                                   boolean addDetailedSources) {
        JavaParser parser = new JavaParser(sourceCode);
        parser.setParserTolerant(false);

        ScanCompilationUnit scanCompilationUnit = new ScanCompilationUnit(summary, runtime);
        org.parsers.java.ast.CompilationUnit cu = parser.CompilationUnit();
        LOGGER.debug("Scanning {}", uri);
        FingerPrint fingerPrint = MD5FingerPrint.compute(sourceCode);
        ScanCompilationUnit.ScanResult sr = scanCompilationUnit.scan(uri, sourceSet, fingerPrint, cu, addDetailedSources);
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
    public List<SourceFile> sourceURIs() {
        return sourceURIs;
    }

    @Override
    public List<SourceFile> testURIs() {
        return testURIs;
    }

    @Override
    public ImportComputer importComputer(int minStar) {
        return runtime.newImportComputer(minStar, packageName -> {
            List<TypeInfo> st = sourceTypeMap.primaryTypesInPackage(packageName);
            if (!st.isEmpty()) return st;
            return compiledTypesManager.primaryTypesInPackage(packageName);
        });
    }

    @Override
    public String print2(TypeInfo typeInfo) {
        OutputBuilder ob = runtime.newTypePrinter(typeInfo, true).print(importComputer(4),
                runtime.qualificationQualifyFromPrimaryType(null), true);
        Formatter formatter = new Formatter2Impl(runtime, new FormattingOptionsImpl.Builder().build());
        return formatter.write(ob);
    }
}

