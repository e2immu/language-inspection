package org.e2immu.language.inspection.api.integration;


import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.parser.SourceTypes;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.InputConfiguration;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/*

 */
public interface JavaInspector {

    void initialize(InputConfiguration inputConfiguration) throws IOException;

    void loadByteCodeQueue();

    void preload(String thePackage);

    Summary parse(boolean failFast);

    // only for testing, after general parse()
    TypeInfo parse(String input);

    // only for testing, after general parse();
    Summary parse(URI typeInfo);

    // only for testing, after general parse();
    List<TypeInfo> parseReturnAll(String input);

    Runtime runtime();

    CompiledTypesManager compiledTypesManager();

    List<URI> sourceURIs();

    List<URI> testURIs();
}
