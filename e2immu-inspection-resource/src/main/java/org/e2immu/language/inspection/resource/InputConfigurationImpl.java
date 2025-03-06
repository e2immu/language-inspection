package org.e2immu.language.inspection.resource;


import org.e2immu.annotation.Container;
import org.e2immu.annotation.Fluent;
import org.e2immu.language.inspection.api.resource.InputConfiguration;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

public record InputConfigurationImpl(List<String> sources,
                                     List<String> restrictSourceToPackages,
                                     List<String> testSources,
                                     List<String> restrictTestSourceToPackages,
                                     List<String> classPathParts,
                                     List<String> runtimeClassPathParts,
                                     List<String> testClassPathParts,
                                     List<String> testRuntimeClassPathParts,
                                     List<String> excludeFromClasspath,
                                     String alternativeJREDirectory,
                                     Charset sourceEncoding,
                                     List<String> dependencies,
                                     boolean infoLogClasspath) implements InputConfiguration {
    public static final String DEFAULT_SOURCE_DIRS = "src/main/java";
    public static final String DEFAULT_TEST_SOURCE_DIRS = "src/test/java";

    public static final String[] DEFAULT_CLASSPATH = {"build/classes/java/main", "jmods/java.base.jmod",
            "jmods/java.xml.jmod", "jmods/java.net.http.jmod"};
    public static final String DEFAULT_CLASSPATH_STRING = String.join(File.pathSeparator, DEFAULT_CLASSPATH);

    static final String NL_TAB = "\n    ";

    @Override
    public InputConfiguration withClassPathParts(String... classPathParts) {
        List<String> combinedClassPathParts = Stream.concat(this.classPathParts.stream(), Arrays.stream(classPathParts))
                .toList();
        return new InputConfigurationImpl(sources, restrictSourceToPackages, testSources, restrictTestSourceToPackages,
                combinedClassPathParts, runtimeClassPathParts, testClassPathParts, testRuntimeClassPathParts,
                excludeFromClasspath, alternativeJREDirectory, sourceEncoding, dependencies, infoLogClasspath);
    }

    @Override
    public String toString() {
        return "InputConfiguration:" +
               NL_TAB + "sources=" + sources +
               NL_TAB + "testSources=" + testSources +
               NL_TAB + "sourceEncoding=" + sourceEncoding.displayName() +
               NL_TAB + "restrictSourceToPackages=" + restrictSourceToPackages +
               NL_TAB + "restrictTestSourceToPackages=" + restrictTestSourceToPackages +
               NL_TAB + "classPathParts=" + classPathParts +
               NL_TAB + "alternativeJREDirectory=" + (alternativeJREDirectory == null ? "<default>" : alternativeJREDirectory);
    }

    @Container
    public static class Builder implements InputConfiguration.Builder {
        private final List<String> sourceDirs = new ArrayList<>();
        private final List<String> testSourceDirs = new ArrayList<>();
        private final List<String> classPathParts = new ArrayList<>();
        private final List<String> runtimeClassPathParts = new ArrayList<>();
        private final List<String> testClassPathParts = new ArrayList<>();
        private final List<String> testRuntimeClassPathParts = new ArrayList<>();
        private final List<String> excludeFromClasspath = new ArrayList<>();

        // result of dependency analysis: group:artifactId:version:configuration
        private final List<String> dependencies = new ArrayList<>();
        private final List<String> restrictSourceToPackages = new ArrayList<>();
        private final List<String> restrictTestSourceToPackages = new ArrayList<>();

        private String alternativeJREDirectory;
        private String sourceEncoding;
        private boolean infoLogClasspath;

        public InputConfiguration build() {
            Charset sourceCharset = sourceEncoding == null ? StandardCharsets.UTF_8 : Charset.forName(sourceEncoding);
            return new InputConfigurationImpl(
                    List.copyOf(sourceDirs),
                    List.copyOf(restrictSourceToPackages),
                    List.copyOf(testSourceDirs),
                    List.copyOf(restrictTestSourceToPackages),
                    List.copyOf(classPathParts),
                    List.copyOf(runtimeClassPathParts),
                    List.copyOf(testClassPathParts),
                    List.copyOf(testRuntimeClassPathParts),
                    List.copyOf(excludeFromClasspath),
                    alternativeJREDirectory,
                    sourceCharset,
                    List.copyOf(dependencies),
                    infoLogClasspath
            );
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
            classPathParts.addAll(Arrays.asList(sources));
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
        public Builder addExcludeFromClasspath(String... jarNames) {
            excludeFromClasspath.addAll(Arrays.asList(jarNames));
            return this;
        }

        @Override
        @Fluent
        public Builder addDependencies(String... deps) {
            dependencies.addAll(Arrays.asList(deps));
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
        public InputConfiguration.Builder setInfoLogClasspath(boolean infoLogClasspath) {
            this.infoLogClasspath = infoLogClasspath;
            return this;
        }
    }
}
