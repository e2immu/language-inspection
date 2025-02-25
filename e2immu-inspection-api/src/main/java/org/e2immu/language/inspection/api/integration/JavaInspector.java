package org.e2immu.language.inspection.api.integration;


import org.e2immu.language.cst.api.info.ImportComputer;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.parser.SourceTypes;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.InputConfiguration;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

/*

 */
public interface JavaInspector {

    ImportComputer importComputer(int i);

    void initialize(InputConfiguration inputConfiguration) throws IOException;

    void loadByteCodeQueue();

    void preload(String thePackage);

    default Summary parse(boolean failFast) {
        return parse(failFast, false);
    }

    default Summary parse(boolean failFast, boolean detailedSources) {
        return parse(failFast, detailedSources, Map.of());
    }

    Summary parse(boolean failFast, boolean detailedSources, Map<String, String> sourcesByTestProtocolURIString);

    // only for testing, after general parse()
    TypeInfo parse(String input);

    default Summary parse(URI typeInfo) {
        return parse(typeInfo, false);
    }

    // only for testing, after general parse();
    Summary parse(URI typeInfo, boolean detailedSources);

    default List<TypeInfo> parseReturnAll(String input) {
        return parseReturnAll(input, false);
    }

    // only for testing, after general parse();
    List<TypeInfo> parseReturnAll(String input, boolean detailedSources);

    String print2(TypeInfo typeInfo);

    Runtime runtime();

    CompiledTypesManager compiledTypesManager();

    List<URI> sourceURIs();

    List<URI> testURIs();
}
