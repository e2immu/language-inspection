package org.e2immu.analyzer.shallow.aapi;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;

public class ShallowAnalyzer {
    private final Runtime runtime;
    private final AnnotationProvider annotationProvider;

    public ShallowAnalyzer(Runtime runtime, AnnotationProvider annotationProvider) {
        this.annotationProvider = annotationProvider;
        this.runtime = runtime;
    }

    public void analyze(TypeInfo typeInfo) {

    }
}
