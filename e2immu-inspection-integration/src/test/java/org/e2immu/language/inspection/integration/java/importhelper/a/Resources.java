package org.e2immu.language.inspection.integration.java.importhelper.a;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Resources {
    Resource[] value();
}
