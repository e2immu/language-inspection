package org.e2immu.language.inspection.api.resource;

import org.e2immu.language.cst.api.element.FingerPrint;
import org.e2immu.language.cst.api.element.SourceSet;

import java.net.URI;

public record SourceFile(String path, URI uri, SourceSet sourceSet, FingerPrint fingerPrint) {

    public static String ensureDotClass(String substring) {
        if (!substring.endsWith(".class")) {
            return substring + ".class";
        }
        return substring;
    }

    public String stripDotClass() {
        return Resources.stripDotClass(path);
    }

    public SourceFile withPath(String path) {
        return new SourceFile(path, uri, sourceSet, fingerPrint);
    }

    public SourceFile withURI(URI uri) {
        return new SourceFile(path, uri, sourceSet, fingerPrint);
    }
}
