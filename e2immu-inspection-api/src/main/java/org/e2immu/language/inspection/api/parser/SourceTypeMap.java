package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.info.TypeInfo;

import java.util.List;

/*
differs from SourceTypes in that:
- it remains mutable during parsing
- it contains types and subtypes
- it is not used for * computation in import statements
 */
public interface SourceTypeMap {

    TypeInfo get(String fullyQualifiedName);

    void invalidate(String fullyQualifiedName);

    List<TypeInfo> primaryTypesInPackage(String packageName);
}
