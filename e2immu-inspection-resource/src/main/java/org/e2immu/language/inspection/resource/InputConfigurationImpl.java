package org.e2immu.language.inspection.resource;


import org.e2immu.annotation.Container;
import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.inspection.api.resource.ClassPathPart;
import org.e2immu.language.inspection.api.resource.InputConfiguration;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record InputConfigurationImpl(List<SourceSet> sourceSets,
                                     List<ClassPathPart> classPathParts,
                                     Path alternativeJREDirectory,
                                     boolean infoLogClasspath) implements InputConfiguration {

    public static final String MAVEN_MAIN = "src/main/java";
    public static final String MAVEN_TEST = "src/test/java";
    public static final String GRADLE_BUIlD = "build/classes/java/main";

    public static final String[] DEFAULT_MODULES = {
            "jmods/java.base.jmod",
            "jmods/java.datatransfer.jmod",
            "jmods/java.desktop.jmod",
            "jmods/java.logging.jmod",
            "jmods/java.net.http.jmod",
            "jmods/java.sql.jmod",
            "jmods/java.xml.jmod",
    };

    public static final String[] GRADLE_DEFAULT = Stream.concat(Stream.of(GRADLE_BUIlD),
            Arrays.stream(DEFAULT_MODULES)).toArray(String[]::new);

    static final String NL_TAB = "\n    ";

    @Override
    public InputConfiguration withClassPathParts(ClassPathPart... classPathParts) {
        return new InputConfigurationImpl(sourceSets, Arrays.stream(classPathParts).toList(), alternativeJREDirectory,
                infoLogClasspath);
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
        private final List<ClassPathPart> classPathParts = new ArrayList<>();
        private final List<String> sourceDirs = new ArrayList<>();
        private final List<String> testSourceDirs = new ArrayList<>();
        private final List<String> classPathStringParts = new ArrayList<>();
        private final List<String> runtimeClassPathParts = new ArrayList<>();
        private final List<String> testClassPathParts = new ArrayList<>();
        private final List<String> testRuntimeClassPathParts = new ArrayList<>();

        private final Set<String> restrictSourceToPackages = new HashSet<>();
        private final Set<String> restrictTestSourceToPackages = new HashSet<>();

        private String alternativeJREDirectory;
        private String sourceEncoding;
        private boolean infoLogClasspath;

        public InputConfiguration build() {
            Charset sourceCharset = sourceEncoding == null ? StandardCharsets.UTF_8 : Charset.forName(sourceEncoding);

            for (String cpp : classPathStringParts) {
                classPathParts.add(new ClassPathPartImpl(cpp, Path.of(cpp), null, false, true,
                        true, isJmod(cpp),
                        Set.of(), Set.of(), URI.create(cpp), false));
            }
            for (String cpp : runtimeClassPathParts) {
                classPathParts.add(new ClassPathPartImpl(cpp, Path.of(cpp), null, false, true,
                        true, isJmod(cpp),
                        Set.of(), Set.of(), URI.create(cpp), true));
            }
            for (String cpp : testClassPathParts) {
                classPathParts.add(new ClassPathPartImpl(cpp, Path.of(cpp), null, true, true,
                        true, isJmod(cpp),
                        Set.of(), Set.of(), URI.create(cpp), false));
            }
            for (String cpp : testRuntimeClassPathParts) {
                classPathParts.add(new ClassPathPartImpl(cpp, Path.of(cpp), null, true, true,
                        true, isJmod(cpp),
                        Set.of(), Set.of(), URI.create(cpp), true));
            }
            for (String sourceDir : sourceDirs) {
                Set<SourceSet> allDependencies = Stream.concat(classPathParts.stream(),
                        sourceSets.stream()).collect(Collectors.toUnmodifiableSet());
                sourceSets.add(new SourceSetImpl(sourceDir, Path.of(sourceDir), sourceCharset, false, false,
                        false, false, restrictSourceToPackages, allDependencies));
            }
            for (String sourceDir : testSourceDirs) {
                Set<SourceSet> allDependencies = Stream.concat(classPathParts.stream(),
                        sourceSets.stream()).collect(Collectors.toUnmodifiableSet());
                sourceSets.add(new SourceSetImpl(sourceDir, Path.of(sourceDir), sourceCharset, true, false,
                        false, false, restrictTestSourceToPackages, allDependencies));
            }
            return new InputConfigurationImpl(List.copyOf(sourceSets), List.copyOf(classPathParts),
                    alternativeJREDirectory == null ? null : Path.of(alternativeJREDirectory),
                    infoLogClasspath);
        }

        private static boolean isJmod(String classPathPart) {
            return classPathPart.startsWith("jmods/");
        }

        @Override
        public Builder addSourceSets(SourceSet... sourceSets) {
            this.sourceSets.addAll(Arrays.asList(sourceSets));
            return this;
        }

        @Override
        public Builder addClassPathParts(ClassPathPart... classPathParts) {
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

        @Override
        public Builder setInfoLogClasspath(boolean infoLogClasspath) {
            this.infoLogClasspath = infoLogClasspath;
            return this;
        }
    }
}
