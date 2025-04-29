package org.e2immu.language.inspection.api.resource;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.SourceSet;

import java.nio.file.Path;
import java.util.List;

public interface InputConfiguration {

    /**
     * At inspection level, the order of the source sets may be important, as packages/types may be ignored
     * when duplicates occur. This duplication needs to be resolved here; the concept of "hidden" or "inactive" types
     * in a source set does not exist at the CST level.
     * If the duplication occurs at package level (as an aggregate over the types), the <code>excludePackages()</code>
     * field in the source set may be used to store this information.
     */
    List<SourceSet> sourceSets();

    /**
     * At inspection level, the order of the class path parts may be important, as packages/types may be ignored
     * when duplicates occur. See <code>sourceSets()</code>.
     */
    List<SourceSet> classPathParts();

    Path alternativeJREDirectory();

    interface Builder {

        @Fluent
        Builder addSourceSets(SourceSet... sourceSets);

        @Fluent
        Builder addClassPathParts(SourceSet... classPathParts);

        // --- alternatives to addSourceSets

        @Fluent
        Builder addSources(String... sources);

        @Fluent
        Builder addRestrictSourceToPackages(String... packages);

        @Fluent
        Builder addRestrictTestSourceToPackages(String... packages);

        // --- alternatives to addClassPathParts

        @Fluent
        Builder addClassPath(String... sources);

        @Fluent
        Builder addRuntimeClassPath(String... sources);

        @Fluent
        Builder addTestClassPath(String... sources);

        @Fluent
        Builder addTestRuntimeClassPath(String... sources);

        @Fluent
        Builder addTestSources(String... sources);

        // --- rest

        @Fluent
        Builder setAlternativeJREDirectory(String alternativeJREDirectory);

        @Fluent
        Builder setSourceEncoding(String sourceEncoding);

        InputConfiguration build();
    }

    // helper

    InputConfiguration withDefaultModules();

}
