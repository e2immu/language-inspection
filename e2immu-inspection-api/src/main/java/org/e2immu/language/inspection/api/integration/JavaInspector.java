package org.e2immu.language.inspection.api.integration;


import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.parser.SourceTypes;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.InputConfiguration;

import java.io.IOException;
import java.util.List;

/*

 */
public interface JavaInspector {

    void initialize(InputConfiguration inputConfiguration) throws IOException;

    void loadByteCodeQueue();

    void preload(String thePackage);

    TypeInfo parse(String input);

    List<TypeInfo> parseReturnAll(String input);

    Runtime runtime();

    CompiledTypesManager compiledTypesManager();

    SourceTypes sourceTypes();
}
