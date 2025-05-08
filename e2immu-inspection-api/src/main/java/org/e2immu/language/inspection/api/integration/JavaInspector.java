package org.e2immu.language.inspection.api.integration;


import org.e2immu.language.cst.api.info.ImportComputer;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.api.resource.SourceFile;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/*

 */
public interface JavaInspector {

    String TEST_PROTOCOL = "test-protocol";

    @FunctionalInterface
    interface Invalidated extends Function<TypeInfo, InvalidationState> {
    }

    record ParseOptions(boolean failFast, boolean detailedSources, boolean allowCreationOfStubTypes,
                        Invalidated invalidated) {
    }

    /*
    Was there a change to this type?
    from high to low in the dependency tree of types: unchanged, invalid/removed, rewire

    REWIRE = the type isn't changed at all, but it accesses invalidated (and hence re-parsed, new) type info objects.
     */
    enum InvalidationState {
        UNCHANGED, INVALID, REWIRE, REMOVED
    }

    interface ParseOptionsBuilder {

        ParseOptionsBuilder setFailFast(boolean failFast);

        ParseOptionsBuilder setDetailedSources(boolean detailedSources);

        ParseOptionsBuilder setAllowCreationOfStubTypes(boolean allowCreationOfStubTypes);

        ParseOptionsBuilder setInvalidated(Invalidated invalidated);

        ParseOptions build();
    }

    ImportComputer importComputer(int minStar);

    record InitializationProblem(String errorMsg, Throwable throwable) {
    }

    List<InitializationProblem> initialize(InputConfiguration inputConfiguration) throws IOException;

    void loadByteCodeQueue();

    void preload(String thePackage);

    // main parse method, from sources specified in InputConfiguration
    Summary parse(ParseOptions parseOptions);

    // only for testing
    Summary parse(Map<String, String> sourcesByTestProtocolURIString, ParseOptions parseOptions);

    // only for testing, uses FAIL_FAST default
    TypeInfo parse(String input);

    // only for testing, after general parse()
    TypeInfo parse(String input, ParseOptions parseOptions);

    // only for testing, after general parse();
    Summary parse(URI typeInfo, ParseOptions parseOptions);

    // only for testing, after general parse();
    List<TypeInfo> parseReturnAll(String input, ParseOptions parseOptions);

    String print2(TypeInfo typeInfo);

    Runtime runtime();

    CompiledTypesManager compiledTypesManager();

    Set<SourceFile> sourceFiles();

    record ReloadResult(List<InitializationProblem> problems, Set<TypeInfo> sourceHasChanged) {}

    ReloadResult reloadSources(InputConfiguration inputConfiguration, Map<String, String> sourcesByTestProtocolURIString) throws IOException;
}
