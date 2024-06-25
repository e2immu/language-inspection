package org.e2immu.language.inspection.api.resource;

import org.e2immu.language.cst.api.info.TypeInfo;

import java.util.List;

public interface ByteCodeInspector {

    /*
        multiple files can be read in one go.
         */
    List<TypeInfo> load(SourceFile sourceFile);
}
