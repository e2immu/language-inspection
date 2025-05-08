package org.e2immu.language.inspection.integration;

import org.e2immu.bytecode.java.asm.ByteCodeInspectorImpl;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.FingerPrint;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.ImportComputer;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.Formatter;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.info.InfoMapImpl;
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

import static org.e2immu.language.inspection.api.integration.JavaInspector.InvalidationState.*;

/*
from input configuration
to classpath + sourceTypeMap/Trie

then, do a 1st round of parsing the source types -> map fqn->type/subType + list compilation unit->parsed object

finally, do the actual parsing for all primary types
 */
public class JavaInspectorImpl implements JavaInspector {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaInspectorImpl.class);

    private Runtime runtime;
    private Map<SourceFile, List<TypeInfo>> sourceFiles;
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
    public static final String JAR_WITH_PATH = "jar-on-classpath";
    public static final String JAR_WITH_PATH_PREFIX = "jar-on-classpath:";
    public static final String E2IMMU_SUPPORT = JAR_WITH_PATH_PREFIX + "org/e2immu/annotation";

    public static final String TEST_PROTOCOL_PREFIX = TEST_PROTOCOL + ":";
    public static final ParseOptions FAIL_FAST = new ParseOptions(true, false,
            false, typeInfo -> UNCHANGED);

    public static class ParseOptionsBuilder implements JavaInspector.ParseOptionsBuilder {
        private boolean failFast;
        private boolean detailedSources;
        private boolean allowCreationOfStubTypes;
        private Invalidated invalidated;

        @Override
        public ParseOptionsBuilder setInvalidated(Invalidated invalidated) {
            this.invalidated = invalidated;
            return this;
        }

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
            return new ParseOptions(failFast, detailedSources, allowCreationOfStubTypes, invalidated);
        }
    }

    @Override
    public List<InitializationProblem> initialize(InputConfiguration inputConfiguration) throws IOException {
        List<InitializationProblem> initializationProblems = new LinkedList<>();
        try {

            Resources classPath = assemblePath(inputConfiguration.workingDirectory(),
                    inputConfiguration.classPathParts(), inputConfiguration.alternativeJREDirectory(),
                    "Classpath", initializationProblems);
            CompiledTypesManagerImpl ctm = new CompiledTypesManagerImpl(classPath);
            runtime = new RuntimeWithCompiledTypesManager(ctm);
            ByteCodeInspector byteCodeInspector = new ByteCodeInspectorImpl(runtime, ctm, computeFingerPrints);
            ctm.setByteCodeInspector(byteCodeInspector);
            this.compiledTypesManager = ctm;

            for (String packageName : new String[]{"java.lang", "java.util.function"}) {
                preload(packageName);
            }

            Resources sourcePath = assemblePath(inputConfiguration.workingDirectory(), inputConfiguration.sourceSets(),
                    inputConfiguration.alternativeJREDirectory(), "Source path", initializationProblems);
            List<SourceFile> sourceFiles = computeSourceURIs(sourcePath);
            this.sourceFiles = new HashMap<>();
            sourceFiles.forEach(sf -> this.sourceFiles.put(sf, List.of()));
        } catch (URISyntaxException e) {
            LOGGER.error("Caught URISyntaxException, transforming into IOException", e);
            throw new IOException(e);
        }
        return List.copyOf(initializationProblems);
    }

    /*
    Strategy:
    Load all sourceFiles from the source path.
    On the basis of the source fingerprints, eep track of
    - types that are new. Action: add to sourceFiles; no need to report, since the code compiles.
    - types that have been removed. Action: remove from sourceFiles; no need to report, since the code compiles.
    - types that have been changed. Report this change, so that the Invalidated parse option can return
        INVALID for this type, and that the dependents can be computed so that they are rewired.
     */
    public ReloadResult reloadSources(InputConfiguration inputConfiguration) throws IOException {
        List<InitializationProblem> initializationProblems = new LinkedList<>();
        Set<TypeInfo> changed = new HashSet<>();
        try {
            Resources sourcePath = assemblePath(inputConfiguration.workingDirectory(), inputConfiguration.sourceSets(),
                    inputConfiguration.alternativeJREDirectory(), "Source path", initializationProblems);
            List<SourceFile> sourceFiles = computeSourceURIs(sourcePath);

            sourceFiles.forEach(sf -> {
                // FIXME ADD CODE
                this.sourceFiles.put(sf, List.of());
            });
        } catch (URISyntaxException e) {
            LOGGER.error("Caught URISyntaxException, transforming into IOException", e);
            throw new IOException(e);
        }
        return new ReloadResult(List.copyOf(initializationProblems), Set.copyOf(changed));
    }

    private List<SourceFile> computeSourceURIs(Resources sourcePath) {
        List<SourceFile> sourceFiles = new LinkedList<>();
        AtomicInteger ignored = new AtomicInteger();
        sourcePath.visit(new String[0], (parts, list) -> {
            if (parts.length >= 1 && !list.isEmpty()) {
                int n = parts.length - 1;
                String name = parts[n];
                if (name.endsWith(".java") && !"package-info.java".equals(name)) {
                    String typeName = name.substring(0, name.length() - 5);
                    String packageName = Arrays.stream(parts).limit(n).collect(Collectors.joining("."));
                    SourceFile sourceFile = list.getFirst();
                    if (sourceFile.sourceSet().acceptSource(packageName, typeName)) {
                        sourceFiles.add(sourceFile);
                        parts[n] = typeName;
                    } else {
                        ignored.incrementAndGet();
                    }
                }
            }
        });
        LOGGER.info("Found {} .java files in {}, skipped {}", sourceFiles.size(), "source path", ignored);
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

    private Resources assemblePath(Path workingDirectory,
                                   List<SourceSet> sourceSets,
                                   Path alternativeJREDirectory,
                                   String msg,
                                   List<InitializationProblem> initializationProblems) throws IOException, URISyntaxException {
        Resources resources = new ResourcesImpl(workingDirectory);
        for (SourceSet sourceSet : sourceSets) {
            String scheme = sourceSet.uri().getScheme();
            Throwable throwable = null;
            String path = sourceSet.uri().getSchemeSpecificPart();
            assert path != null && !path.isBlank();
            switch (scheme) {
                case JAR_WITH_PATH -> {
                    URL jarUrl = resources.findJarInClassPath(path);
                    if (jarUrl == null) {
                        String msgString = msg + " part '" + sourceSet.uri() + "': jar not found";
                        LOGGER.warn(msg);
                        initializationProblems.add(new InitializationProblem(msgString, null));
                    } else {
                        addJar(resources, path, jarUrl, sourceSet);
                    }
                }
                case "jmod" -> {
                    try {
                        URL url = ResourcesImpl.constructJModURL(path, alternativeJREDirectory);
                        FingerPrint fingerPrint = makeFingerPrint(url);
                        sourceSet.setFingerPrint(fingerPrint);
                        int entries = resources.addJmod(new SourceFile(path, url.toURI(), sourceSet, null));
                        LOGGER.debug("Added {} entries for jmod {}", entries, path);
                    } catch (IOException e) {
                        throwable = e;
                    }
                }
                case TEST_PROTOCOL -> resources.addTestProtocol(new SourceFile(path, sourceSet.uri(), sourceSet,
                        null));
                case "file" -> {
                    File file = toAbsoluteFile(workingDirectory, path);
                    if (path.endsWith(".jar")) {
                        try {
                            // "jar:file:build/libs/equivalent.jar!/"
                            URL jarUrl = URI.create("jar:file:" + file.getPath() + "!/").toURL();
                            addJar(resources, path, jarUrl, sourceSet);
                        } catch (IOException e) {
                            throwable = e;
                        }
                    } else {
                        if (file.isDirectory()) {
                            LOGGER.info("Adding {} to {}", file.getAbsolutePath(), msg);
                            resources.addDirectoryFromFileSystem(file, sourceSet);
                        } else {
                            String msgString = msg + " part '" + path + "' is not a .jar file, and not a directory: ignored";
                            LOGGER.warn(msgString);
                            initializationProblems.add(new InitializationProblem(msgString, null));
                        }
                    }
                }
                case null, default -> throw new UnsupportedOperationException("Unknown URI scheme " + scheme);
            }
            if (throwable != null) {
                String msgString = msg + " part '" + path + "' ignored: " + throwable.getMessage();
                LOGGER.warn(msgString);
                initializationProblems.add(new InitializationProblem(msgString, throwable));
            }
        }
        return resources;
    }

    private File toAbsoluteFile(Path workingDirectory, String path) {
        File file = new File(path);
        if (file.exists() && file.isAbsolute()) return file;
        return new File(workingDirectory.toFile(), path);
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
        return parseReturnAll(input, parseOptions).getFirst();
    }

    @Override
    public TypeInfo parse(String input) {
        return parseReturnAll(input, FAIL_FAST).getFirst();
    }

    @Override
    public List<TypeInfo> parseReturnAll(String input, ParseOptions parseOptions) {
        Summary failFastSummary = new SummaryImpl(true);
        try {
            URI uri = new URI("input");
            SourceFile sourceFile = new SourceFile(null, uri, null, MD5FingerPrint.compute(input));
            return internalParseSingleInput(failFastSummary, sourceFile, () -> {
                JavaParser parser = new JavaParser(input);
                parser.setParserTolerant(false);
                return parser;
            }, parseOptions);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    // not the primary endpoint!
    private List<TypeInfo> internalParseSingleInput(Summary summary,
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
            internalParseSingleInput(summary, sourceFile, () -> {
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

    private record SourceFileCompilationUnit(SourceFile sourceFile,
                                             org.parsers.java.ast.CompilationUnit cu,
                                             CompilationUnit parsedCu) {
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

        Invalidated invalidated = parseOptions.invalidated();

        List<SourceFile> sourceFilesToParse = new ArrayList<>(sourceFiles.size());
        Set<TypeInfo> typesToRewire = new HashSet<>();
        this.sourceFiles.forEach((sf, typeInfos) -> {
            if (typeInfos.isEmpty()) {
                sourceFilesToParse.add(sf);
            } else {
                // TODO if there are multiple primary types here, and only one is invalid, we must make sure that
                //   all the descendants of the other must be rewired. This is an edge case.
                if (typeInfos.stream().anyMatch(ti -> invalidated.apply(ti) == INVALID)) {
                    typeInfos.forEach(ti -> sourceTypeMap.invalidate(ti.fullyQualifiedName()));
                    sourceFilesToParse.add(sf);
                } else {
                    typeInfos.forEach(ti -> {
                        InvalidationState state = invalidated.apply(ti);
                        if (state == UNCHANGED) {
                            summary.addType(ti, true);
                        } else if (state == REWIRE) {
                            typesToRewire.add(ti);
                        }
                    });
                }
            }
        });
        List<SourceFileCompilationUnit> list = new ArrayList<>(sourceFilesToParse.size());

        for (SourceFile sourceFile : sourceFilesToParse) {
            String uriString = sourceFile.uri().toString();
            SourceFileCompilationUnit sfCu;
            if (uriString.startsWith(TEST_PROTOCOL_PREFIX)) {
                String sourceCode = sourcesByTestProtocolURIString.get(uriString);
                sfCu = parseSourceString(sourceFile, sourceFile.sourceSet(), sourceCode, summary,
                        parseOptions.detailedSources());
                list.add(sfCu);
            } else {
                try (InputStreamReader isr = makeInputStreamReader(sourceFile.uri()); StringWriter sw = new StringWriter()) {
                    isr.transferTo(sw);
                    String sourceCode = sw.toString();
                    sfCu = parseSourceString(sourceFile, sourceFile.sourceSet(), sourceCode, summary,
                            parseOptions.detailedSources());
                    list.add(sfCu);
                } catch (IOException io) {
                    LOGGER.error("Caught IO exception", io);
                    summary.addParserError(io);
                }
            }
        }

        // PHASE 2: actual parsing of types, methods, fields

        InfoMap infoMap = new InfoMapImpl(typesToRewire);
        for (SourceFileCompilationUnit sfCu : list) {
            ParseCompilationUnit parseCompilationUnit = new ParseCompilationUnit(rootContext);
            LOGGER.debug("Parsing {}", sfCu.sourceFile().uri());
            List<TypeInfo> types = parseCompilationUnit.parse(sfCu.parsedCu, sfCu.cu);
            types.forEach(ti -> {
                summary.addType(ti, true);
                infoMap.put(ti);
            });
            sourceFiles.put(sfCu.sourceFile, List.copyOf(types));
        }

        for (TypeInfo typeInfo : typesToRewire) {
            TypeInfo rewired = infoMap.typeInfoRecurseAllPhases(typeInfo);
            sourceTypeMap.put(rewired);
            summary.addType(rewired, true);
        }

        // PHASE 3: resolving: content of methods, field initializers

        rootContext.resolver().resolve();
        return summary;
    }

    private SourceFileCompilationUnit parseSourceString(SourceFile sourceFile, SourceSet sourceSet, String sourceCode,
                                                        Summary summary, boolean addDetailedSources) {
        assert sourceCode != null;
        JavaParser parser = new JavaParser(sourceCode);
        parser.setParserTolerant(false);

        ScanCompilationUnit scanCompilationUnit = new ScanCompilationUnit(summary, runtime);
        org.parsers.java.ast.CompilationUnit cu = parser.CompilationUnit();
        LOGGER.debug("Scanning {}", sourceFile.uri());
        FingerPrint fingerPrint = MD5FingerPrint.compute(sourceCode);
        ScanCompilationUnit.ScanResult sr = scanCompilationUnit.scan(sourceFile.uri(), sourceSet, fingerPrint, cu,
                addDetailedSources);
        sourceTypeMap.putAll(sr.sourceTypes());
        CompilationUnit compilationUnit = sr.compilationUnit();
        return new SourceFileCompilationUnit(sourceFile, cu, compilationUnit);
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
    public Set<SourceFile> sourceFiles() {
        return sourceFiles.keySet();
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

