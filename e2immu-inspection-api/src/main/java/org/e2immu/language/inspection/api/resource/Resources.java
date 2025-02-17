package org.e2immu.language.inspection.api.resource;

import org.e2immu.language.cst.api.info.TypeInfo;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public interface Resources {

    // convert input strings to resources represented by URIs; return InputPathEntry summary.

    InputPathEntry addDirectoryFromFileSystem(String originalInput, File file);

    class JarNotFoundException extends RuntimeException {}

    InputPathEntry addJarFromClassPath(String packagePrefix) throws IOException;

    InputPathEntry addTestProtocol(String testProtocol);

    InputPathEntry addJarFromFileSystem(String originalInput);

    InputPathEntry addJar(String originalInput, URL jarUrl);

    InputPathEntry addJmodFromFileSystem(String originalInput, String alternativeJRELocation);

    InputPathEntry addJmod(String originalInput, URL jmodUrl);

    static String stripDotClass(String path) {
        if (path.endsWith(".class")) return path.substring(0, path.length() - 6);
        return path;
    }

    // work with input URIs

    String pathToFqn(String name);

    SourceFile sourceFileOfType(TypeInfo subType, String s);

    void visit(String[] prefix, BiConsumer<String[], List<URI>> visitor);

    List<String[]> expandPaths(String path);

    void expandPaths(String path, String extension, BiConsumer<String[], List<URI>> visitor);

    void expandLeaves(String path, String extension, BiConsumer<String[], List<URI>> visitor);

    List<URI> expandURLs(String extension);

    SourceFile fqnToPath(String fqn, String s);

    byte[] loadBytes(String path);
}
