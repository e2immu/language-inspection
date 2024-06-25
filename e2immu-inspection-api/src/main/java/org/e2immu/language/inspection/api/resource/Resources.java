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

    static String stripDotClass(String path) {
        if (path.endsWith(".class")) return path.substring(0, path.length() - 6);
        return path;
    }

    void addDirectoryFromFileSystem(File base);

    String pathToFqn(String name);

    SourceFile sourceFileOfSubType(TypeInfo subType, String s);

    record JarSize(int entries, int bytes) {
    }

    Map<String, Resources.JarSize> getJarSizes();

    void visit(String[] prefix, BiConsumer<String[], List<URI>> visitor);

    List<String[]> expandPaths(String path);

    void expandPaths(String path, String extension, BiConsumer<String[], List<URI>> visitor);

    void expandLeaves(String path, String extension, BiConsumer<String[], List<URI>> visitor);

    List<URI> expandURLs(String extension);

    Map<String, Integer> addJarFromClassPath(String prefix) throws IOException;

    int addJar(URL jarUrl) throws IOException;

    int addJmod(URL url) throws IOException;

    SourceFile fqnToPath(String fqn, String s);

    byte[] loadBytes(String path);
}
