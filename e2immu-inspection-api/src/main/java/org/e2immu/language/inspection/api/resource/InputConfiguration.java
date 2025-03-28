package org.e2immu.language.inspection.api.resource;

import org.e2immu.annotation.Fluent;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface InputConfiguration {

    boolean infoLogClasspath();

    List<String> sources();

    List<String> restrictSourceToPackages();

    List<String> testSources();

    List<String> restrictTestSourceToPackages();

    List<String> classPathParts();

    List<String> runtimeClassPathParts();

    List<String> testClassPathParts();

    List<String> testRuntimeClassPathParts();

    List<String> excludeFromClasspath();

    String alternativeJREDirectory();

    Charset sourceEncoding();

    List<String> dependencies();

    InputConfiguration withClassPathParts(String... classPathParts);

    interface Builder {

        @Fluent
        Builder addClassPath(String... sources);

        @Fluent
        Builder addExcludeFromClasspath(String... jarNames);

        @Fluent
        Builder addDependencies(String... deps);

        @Fluent
        Builder addRestrictSourceToPackages(String... packages);

        @Fluent
        Builder addRestrictTestSourceToPackages(String... packages);

        @Fluent
        Builder addRuntimeClassPath(String... sources);

        @Fluent
        Builder addSources(String... sources);

        @Fluent
        Builder addTestClassPath(String... sources);

        @Fluent
        Builder addTestRuntimeClassPath(String... sources);

        @Fluent
        Builder addTestSources(String... sources);

        @Fluent
        Builder setAlternativeJREDirectory(String alternativeJREDirectory);

        @Fluent
        Builder setSourceEncoding(String sourceEncoding);

        @Fluent
        Builder setInfoLogClasspath(boolean infoLogClasspath);

        InputConfiguration build();
    }

    default List<String> allClassPathParts() {
        return Stream.concat(Stream.concat(Stream.concat(classPathParts().stream(),
                        testClassPathParts().stream()), runtimeClassPathParts().stream()),
                testRuntimeClassPathParts().stream()).distinct().toList();
    }
}
