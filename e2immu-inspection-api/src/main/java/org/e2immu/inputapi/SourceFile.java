package org.e2immu.inputapi;

import java.net.URI;

public record SourceFile(String path, URI uri) {

    public static String ensureDotClass(String substring) {
        if (!substring.endsWith(".class")) {
            return substring + ".class";
        }
        return substring;
    }

    public String stripDotClass() {
        return Resources.stripDotClass(path);
    }
}
