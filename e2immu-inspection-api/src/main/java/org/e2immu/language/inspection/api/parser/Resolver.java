package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;

public interface Resolver {
    /*
    we must add the ECI here, because CongoCC does not see the ECI as a separate statement
     */
    void add(Info.Builder<?> infoBuilder, ForwardType forwardType,
             Object eci,
             Object expression, Context context);

    void add(TypeInfo.Builder typeInfoBuilder);

    void resolve();
}
