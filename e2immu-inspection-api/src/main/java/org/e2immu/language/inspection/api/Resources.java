package org.e2immu.language.inspection.api;

public interface Resources {

    static String stripDotClass(String path) {
        if (path.endsWith(".class")) return path.substring(0, path.length() - 6);
        return path;
    }

    SourceFile fqnToPath(String fqn, String s);

    byte[] loadBytes(String path);
}
