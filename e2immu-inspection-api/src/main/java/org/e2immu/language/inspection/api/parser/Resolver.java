package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;

public interface Resolver {
    void add(Info info,
             Info.Builder<?> infoBuilder,
             ForwardType forwardType,
             Object explicitConstructorInvocation,
             Object toResolve,
             Context newContext);

    void add(TypeInfo.Builder builder);

    // add to the to-do list, but only for overrides
    void addRecordAccessor(MethodInfo accessor);

    Resolver newEmpty();

    void resolve();

    ParseHelper parseHelper();
}
