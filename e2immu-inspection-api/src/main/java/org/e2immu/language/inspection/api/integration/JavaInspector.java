package org.e2immu.language.inspection.api.integration;


import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.parser.SourceTypes;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.InputConfiguration;

import java.io.IOException;

/*

 */
public interface JavaInspector {

    void initialize(InputConfiguration inputConfiguration) throws IOException;

    void loadByteCodeQueue();

    void preload(String thePackage);

    Runtime runtime();

    CompiledTypesManager compiledTypesManager();

    SourceTypes sourceTypes();
}
