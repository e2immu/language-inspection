package org.e2immu.language.inspection.api.resource;

import org.e2immu.language.cst.api.element.SourceSet;

import java.net.URI;

public interface ClassPathPart extends SourceSet {

    URI uri();

    // test() is already there
    boolean runtimeOnly();

}
