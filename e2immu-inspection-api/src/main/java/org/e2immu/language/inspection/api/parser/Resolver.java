package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;

public interface Resolver {
    void add(Info.Builder<?> infoBuilder, ForwardType forwardType, Object explicitConstructorInvocation, Object toResolve, Context newContext);

    void add(TypeInfo.Builder builder);

    void resolve();
}
