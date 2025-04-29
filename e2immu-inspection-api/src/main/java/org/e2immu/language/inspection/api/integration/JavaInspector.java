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

/*

 */
public interface JavaInspector {

    String TEST_PROTOCOL = "test-protocol";

    record ParseOptions(boolean failFast, boolean detailedSources, boolean allowCreationOfStubTypes) {
    }

    interface ParseOptionsBuilder {

        ParseOptionsBuilder setFailFast(boolean failFast);

        ParseOptionsBuilder setDetailedSources(boolean detailedSources);

        ParseOptionsBuilder setAllowCreationOfStubTypes(boolean allowCreationOfStubTypes);

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

    List<SourceFile> sourceURIs();

    List<SourceFile> testURIs();
}
