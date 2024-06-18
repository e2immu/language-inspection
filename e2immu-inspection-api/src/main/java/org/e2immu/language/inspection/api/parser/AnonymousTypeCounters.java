package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.info.TypeInfo;

public interface AnonymousTypeCounters {
    int newIndex(TypeInfo typeInfo);
}
