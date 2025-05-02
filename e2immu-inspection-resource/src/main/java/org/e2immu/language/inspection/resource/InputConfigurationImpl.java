package org.e2immu.language.inspection.resource;

import org.e2immu.annotation.Container;
import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.inspection.api.resource.InputConfiguration;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record InputConfigurationImpl(Path workingDirectory,
                                     List<SourceSet> sourceSets,
                                     List<SourceSet> classPathParts,
                                     Path alternativeJREDirectory) implements InputConfiguration {

    public static final String MAVEN_MAIN = "src/main/java";
    public static final String MAVEN_TEST = "src/test/java";
    public static final String GRADLE_BUIlD = "build/classes/java/main";

    public static final String[] DEFAULT_MODULES = {
            "jmod:java.base",
            "jmod:java.datatransfer",
            "jmod:java.desktop",
            "jmod:java.logging",
            "jmod:java.net.http",
            "jmod:java.sql",
            "jmod:java.xml",
    };

    public static final String[] GRADLE_DEFAULT = Stream.concat(Stream.of(GRADLE_BUIlD),
            Arrays.stream(DEFAULT_MODULES)).toArray(String[]::new);

    static final String NL_TAB = "\n    ";

    @Override
    public InputConfiguration withDefaultModules() {
        Stream<SourceSet> defaultModuleStream = Arrays.stream(DEFAULT_MODULES).map(mod ->
                new SourceSetImpl(mod, null, URI.create(mod), StandardCharsets.UTF_8, false, true,
                        false, true, false, Set.of(), Set.of()));
        return new InputConfigurationImpl(workingDirectory, sourceSets, Stream.concat(classPathParts.stream(),
                defaultModuleStream).toList(), alternativeJREDirectory);
    }

    @Override
    public String toString() {
        return "InputConfiguration:" +
               NL_TAB + "sourcesSets=" + sourceSets +
               NL_TAB + "classPathParts=" + classPathParts +
               NL_TAB + "alternativeJREDirectory=" + (alternativeJREDirectory == null ? "<default>"
                : alternativeJREDirectory);
    }

    @Container
    public static class Builder implements InputConfiguration.Builder {
        private final List<SourceSet> sourceSets = new ArrayList<>();
        private final List<SourceSet> classPathParts = new ArrayList<>();
        private final List<String> sourceDirs = new ArrayList<>();
        private final List<String> testSourceDirs = new ArrayList<>();
        private final List<String> classPathStringParts = new ArrayList<>();
        private final List<String> runtimeClassPathParts = new ArrayList<>();
        private final List<String> testClassPathParts = new ArrayList<>();
        private final List<String> testRuntimeClassPathParts = new ArrayList<>();

        private final Set<String> restrictSourceToPackages = new HashSet<>();
        private final Set<String> restrictTestSourceToPackages = new HashSet<>();

        private String workingDirectory;
        private String alternativeJREDirectory;
        private String sourceEncoding;

        public InputConfiguration build() {
            Charset sourceCharset = sourceEncoding == null ? StandardCharsets.UTF_8 : Charset.forName(sourceEncoding);

            for (String cpp : classPathStringParts) {
                classPathParts.add(new SourceSetImpl(cpp, null, createURI(cpp), null,
                        false, true, true, isJmod(cpp), false, Set.of(), Set.of()));
            }
            for (String cpp : runtimeClassPathParts) {
                classPathParts.add(new SourceSetImpl(cpp, null, createURI(cpp), null,
                        false, true, true, isJmod(cpp), true, Set.of(), Set.of()));
            }
            for (String cpp : testClassPathParts) {
                classPathParts.add(new SourceSetImpl(cpp, null, createURI(cpp), null,
                        true, true, true, isJmod(cpp), false, Set.of(), Set.of()));
            }
            for (String cpp : testRuntimeClassPathParts) {
                classPathParts.add(new SourceSetImpl(cpp, null, createURI(cpp), null,
                        true, true, true, isJmod(cpp), true, Set.of(), Set.of()));
            }
            for (String sourceDir : sourceDirs) {
                Set<SourceSet> allDependencies = Stream.concat(classPathParts.stream(),
                        sourceSets.stream()).collect(Collectors.toUnmodifiableSet());
                sourceSets.add(new SourceSetImpl(sourceDir, List.of(Path.of(sourceDir)), createURI(sourceDir), sourceCharset,
                        false, false, false, false, false,
                        restrictSourceToPackages, allDependencies));
            }
            for (String sourceDir : testSourceDirs) {
                Set<SourceSet> allDependencies = Stream.concat(classPathParts.stream(),
                        sourceSets.stream()).collect(Collectors.toUnmodifiableSet());
                sourceSets.add(new SourceSetImpl(sourceDir, List.of(Path.of(sourceDir)), createURI(sourceDir), sourceCharset,
                        true, false, false, false, false,
                        restrictTestSourceToPackages, allDependencies));
            }
            return new InputConfigurationImpl(workingDirectory == null || workingDirectory.isBlank()
                    ? Path.of(".") : Path.of(workingDirectory),
                    List.copyOf(sourceSets), List.copyOf(classPathParts),
                    alternativeJREDirectory == null || alternativeJREDirectory.isBlank()
                            ? null : Path.of(alternativeJREDirectory));
        }

        private static final Pattern SCHEME = Pattern.compile("([A-Za-z-]+):.+");

        private static URI createURI(String path) {
            if (SCHEME.matcher(path).matches()) {
                return URI.create(path);
            }
            return URI.create("file:" + path);
        }

        private static boolean isJmod(String classPathPart) {
            return classPathPart.startsWith("jmod:");
        }

        @Override
        public Builder setWorkingDirectory(String workingDirectory) {
            this.workingDirectory = workingDirectory;
            return this;
        }

        @Override
        public Builder addSourceSets(SourceSet... sourceSets) {
            this.sourceSets.addAll(Arrays.asList(sourceSets));
            return this;
        }

        @Override
        public Builder addClassPathParts(SourceSet... classPathParts) {
            this.classPathParts.addAll(Arrays.asList(classPathParts));
            return this;
        }

        @Override
        @Fluent
        public Builder addSources(String... sources) {
            sourceDirs.addAll(Arrays.asList(sources));
            return this;
        }

        @Override
        @Fluent
        public Builder addTestSources(String... sources) {
            testSourceDirs.addAll(Arrays.asList(sources));
            return this;
        }

        @Override
        @Fluent
        public Builder addClassPath(String... sources) {
            classPathStringParts.addAll(Arrays.asList(sources));
            return this;
        }

        @Override
        @Fluent
        public Builder addRuntimeClassPath(String... sources) {
            runtimeClassPathParts.addAll(Arrays.asList(sources));
            return this;
        }

        @Override
        @Fluent
        public Builder addTestClassPath(String... sources) {
            testClassPathParts.addAll(Arrays.asList(sources));
            return this;
        }

        @Override
        @Fluent
        public Builder addTestRuntimeClassPath(String... sources) {
            testRuntimeClassPathParts.addAll(Arrays.asList(sources));
            return this;
        }

        @Override
        @Fluent
        public Builder setAlternativeJREDirectory(String alternativeJREDirectory) {
            this.alternativeJREDirectory = alternativeJREDirectory;
            return this;
        }

        @Override
        @Fluent
        public Builder setSourceEncoding(String sourceEncoding) {
            this.sourceEncoding = sourceEncoding;
            return this;
        }

        @Override
        @Fluent
        public Builder addRestrictSourceToPackages(String... packages) {
            restrictSourceToPackages.addAll(Arrays.asList(packages));
            return this;
        }

        @Override
        @Fluent
        public Builder addRestrictTestSourceToPackages(String... packages) {
            restrictTestSourceToPackages.addAll(Arrays.asList(packages));
            return this;
        }
    }
}
