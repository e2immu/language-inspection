package org.e2immu.language.inspection.resource;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.resource.InputPathEntry;
import org.e2immu.language.inspection.api.resource.Resources;
import org.e2immu.language.inspection.api.resource.SourceFile;
import org.e2immu.util.internal.util.Trie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourcesImpl implements Resources {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcesImpl.class);
    private final boolean computeHashes;

    public ResourcesImpl(boolean computeHashes) {
        this.computeHashes = computeHashes;
    }

    static class ResourceAccessException extends RuntimeException {
        public ResourceAccessException(String msg) {
            super(msg);
        }
    }

    private final Trie<URI> data = new Trie<>();

    @Override
    public void visit(String[] prefix, BiConsumer<String[], List<URI>> visitor) {
        data.visit(prefix, visitor);
    }

    @Override
    public List<String[]> expandPaths(String path) {
        List<String[]> expansions = new LinkedList<>();
        String[] prefix = path.split("\\.");
        data.visit(prefix, (s, list) -> expansions.add(s));
        return expansions;
    }

    @Override
    public void expandPaths(String path, String extension, BiConsumer<String[], List<URI>> visitor) {
        String[] prefix = path.split("\\.");
        data.visit(prefix, (s, list) -> {
            if (s.length > 0 && s[s.length - 1].endsWith(extension)) {
                visitor.accept(s, list);
            }
        });
    }

    @Override
    public void expandLeaves(String path, String extension, BiConsumer<String[], List<URI>> visitor) {
        String[] prefix = path.split("\\.");
        data.visitLeaves(prefix, (s, list) -> {
            if (s.length > 0 && s[s.length - 1].endsWith(extension)) {
                visitor.accept(s, list);
            }
        });
    }

    @Override
    public List<URI> expandURLs(String extension) {
        List<URI> expansions = new LinkedList<>();
        data.visit(new String[0], (s, list) -> {
            if (s[s.length - 1].endsWith(extension)) {
                expansions.addAll(list);
            }
        });
        return expansions;
    }

    /**
     * @param prefix add the first jar that contains the package denoted by the prefix
     */
    @Override
    public InputPathEntry addJarFromClassPath(String prefix) {
        try {
            Enumeration<URL> roots = getClass().getClassLoader().getResources(prefix);
            if (roots.hasMoreElements()) {
                URL url = roots.nextElement();
                String urlString = url.toString();
                int bangSlash = urlString.indexOf("!/");
                String strippedUrlString = urlString.substring(0, bangSlash + 2);
                URL strippedURL = new URL(strippedUrlString);
                LOGGER.debug("Stripped URL is {}", strippedURL);
                if ("jar".equals(strippedURL.getProtocol())) {
                    return addJar(prefix, strippedURL);
                }
                Exception e = new MalformedURLException("Protocol not implemented in URL: " + strippedURL.getProtocol());
                return new InputPathEntryImpl.Builder(prefix).addException(e).build();
            }
        } catch (IOException ioe) {
            return new InputPathEntryImpl.Builder(prefix).addException(ioe).build();
        }
        return new InputPathEntryImpl.Builder(prefix).addException(new JarNotFoundException()).build();
    }

    private static final Pattern JAR_FILE = Pattern.compile("/([^/]+\\.jar)");

    @Override
    public InputPathEntry addTestProtocol(String testProtocol) {
        InputPathEntryImpl.Builder builder = new InputPathEntryImpl.Builder(testProtocol);
        try {
            URI uri = new URI(testProtocol);
            String packageName = testProtocol.substring(testProtocol.indexOf(':') + 1, testProtocol.indexOf('/'));
            builder.addPackage(packageName);
            String[] split = packageName.split("\\.");
            split[split.length - 1] = split[split.length - 1] + ".java";
            data.add(split, uri);
            return builder.setTypeCount(1).setURI(uri).build();
        } catch (URISyntaxException e) {
            LOGGER.error("Caught exception in addTestProtocol, with input {}", testProtocol, e);
            return builder.addException(e).build();
        }
    }

    @Override
    public InputPathEntry addJarFromFileSystem(String originalInput) {
        File file = new File(originalInput);
        try {
            URL url = new URL("jar:file:" + originalInput + "!/");
            InputPathEntry entry = addJar(originalInput, url).withByteCount((int) file.length());
            if (computeHashes) {
                try {
                    String hash = md5HashString(file);
                    return entry.withHash(hash);
                } catch (IOException | NoSuchAlgorithmException e) {
                    LOGGER.error("Caught exception in addJarFromFileSystem, {}", originalInput, e);
                    return entry.withException(e);
                }
            }
            return entry;
        } catch (MalformedURLException e) {
            LOGGER.error("Caught exception in addJarFromFileSystem, {}", originalInput, e);
            return new InputPathEntryImpl.Builder(originalInput).addException(e).build();
        }
    }

    private static String md5HashString(File file) throws IOException, NoSuchAlgorithmException {
        byte[] data = Files.readAllBytes(file.toPath());
        byte[] hash = MessageDigest.getInstance("MD5").digest(data);
        return new BigInteger(1, hash).toString(16);
    }

    /**
     * Add a jar to the trie
     *
     * @param jarUrl must be a correct JAR url, as described in the class JarURLConnection
     * @return the number of entries added to the classpath
     */
    @Override
    public InputPathEntry addJar(String originalInput, URL jarUrl) {
        return addJar(originalInput, jarUrl,
                je -> je.getRealName().endsWith(".class") || je.getRealName().endsWith(".java"),
                JarEntry::getRealName);
    }

    private InputPathEntry addJar(String originalInput,
                                  URL jarUrl,
                                  Predicate<JarEntry> filter,
                                  Function<JarEntry, String> realNameFunction) {
        InputPathEntryImpl.Builder builder = new InputPathEntryImpl.Builder(originalInput);
        try {
            builder.setURI(jarUrl.toURI());
        } catch (URISyntaxException e) {
            LOGGER.error("Caught exception in addJar, input {}", jarUrl, e);
            return builder.addException(e).build();
        }
        try {
            JarURLConnection jarConnection = (JarURLConnection) jarUrl.openConnection();
            JarFile jarFile = jarConnection.getJarFile();
            AtomicInteger entries = new AtomicInteger();
            jarFile.stream().filter(filter).forEach(je -> {
                String realName = realNameFunction.apply(je);
                LOGGER.trace("Adding {}", realName);
                String[] split = je.getRealName().split("/");
                try {
                    URI fullUrl = new URL(jarUrl, je.getRealName()).toURI();
                    data.add(split, fullUrl);
                    entries.incrementAndGet();
                    String packageName = Arrays.stream(split)
                            .limit(split.length - 1)
                            .collect(Collectors.joining("."));
                    builder.addPackage(packageName);
                } catch (MalformedURLException | URISyntaxException e) {
                    LOGGER.error("Caught exception in addJar, {}, adding {}", originalInput, realName, e);
                    builder.addException(e);
                }
            });
            builder.setTypeCount(entries.get());
        } catch (IOException e) {
            LOGGER.error("Caught exception in addJar, {}", originalInput, e);
            builder.addException(e);
        }
        return builder.build();
    }

    @Override
    public InputPathEntry addJmodFromFileSystem(String originalInput, String alternativeJRELocation) {
        try {
            File file = constructJModURL(originalInput, alternativeJRELocation);
            URL url = new URL("jar:file:" + file + "!/");
            InputPathEntry entry = addJmod(originalInput, url).withByteCount((int) file.length());
            if (computeHashes) {
                try {
                    String hash = md5HashString(file);
                    return entry.withHash(hash);
                } catch (IOException | NoSuchAlgorithmException e) {
                    LOGGER.error("Caught exception in addJmodFromFileSystem, {}", originalInput, e);
                    return entry.withException(e);
                }
            }
            return entry;
        } catch (MalformedURLException e) {
            LOGGER.error("Caught exception in addJmodFromFileSystem, {}", originalInput, e);
            return new InputPathEntryImpl.Builder(originalInput).addException(e).build();
        }
    }

    @Override
    public InputPathEntry addJmod(String originalInput, URL jmodUrl) {
        return addJar(originalInput, jmodUrl, je -> je.getRealName().startsWith("classes/"),
                je -> je.getRealName().substring(8));
    }

    private static File constructJModURL(String part, String altJREDirectory) throws MalformedURLException {
        if (part.startsWith("/")) {
            return new File(part);
        }
        String jre;
        if (altJREDirectory == null) {
            jre = System.getProperty("java.home");
        } else {
            jre = altJREDirectory;
        }
        if (!jre.endsWith("/")) return new File(jre + "/");
        return new File(jre);
    }

    @Override
    public SourceFile sourceFileOfType(TypeInfo subType, String suffix) {
        if (subType.compilationUnitOrEnclosingType().isLeft()) {
            String path = subType.fullyQualifiedName().replace(".", "/") + suffix;
            return new SourceFile(path, subType.compilationUnit().uri());
        }
        SourceFile parentSourceFile = sourceFileOfType(subType.compilationUnitOrEnclosingType().getRight(), suffix);
        String p = parentSourceFile.path();
        String newPath = p.substring(0, p.length() - suffix.length()) + "$" + subType.simpleName() + suffix;
        return new SourceFile(newPath, parentSourceFile.uri());
    }

    @Override
    public SourceFile fqnToPath(String fqn, String extension) {
        String[] splitDot = fqn.split("\\.");
        for (int i = 1; i < splitDot.length; i++) {
            String[] parts = new String[i + 1];
            System.arraycopy(splitDot, 0, parts, 0, i);
            parts[i] = splitDot[i];
            for (int j = i + 1; j < splitDot.length; j++) {
                parts[i] += "$" + splitDot[j];
            }
            parts[i] += extension;
            List<URI> uris = data.get(parts);
            if (uris != null && !uris.isEmpty()) {
                return new SourceFile(String.join("/", parts), uris.get(0));
            }
        }
        LOGGER.debug("Cannot find {} with extension {} in classpath", fqn, extension);
        return null;
    }

    // could have been static, but allows for overrides
    @Override
    public String pathToFqn(String path) {
        String stripDotClass = Resources.stripDotClass(path);
        if (stripDotClass.endsWith("$")) {
            // scala
            return stripDotClass.substring(0, stripDotClass.length() - 1).replaceAll("[/$]", ".") + ".object";
        }
        if (stripDotClass.endsWith("$class")) {
            // scala; keep it as is, ending in .class
            return stripDotClass.replaceAll("[/$]", ".");
        }
        int anon;
        if ((anon = stripDotClass.indexOf("$$anonfun")) > 0) {
            // scala
            String random = Integer.toString(Math.abs(stripDotClass.hashCode()));
            return stripDotClass.substring(0, anon).replaceAll("[/$]", ".") + "." + random;
        }
        return stripDotClass.replaceAll("[/$]", ".");
    }

    @Override
    public byte[] loadBytes(String path) {
        String[] prefix = path.split("/");
        List<URI> urls = data.get(prefix);
        if (urls != null) {
            for (URI uri : urls) {
                try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                     InputStream inputStream = uri.toURL().openStream()) {
                    inputStream.transferTo(byteArrayOutputStream);
                    return byteArrayOutputStream.toByteArray();
                } catch (IOException e) {
                    throw new ResourceAccessException("URL = " + uri + ", Cannot read? " + e.getMessage());
                }
            }
        }
        LOGGER.debug("{} not found in class path", path);
        return null;
    }

    @Override
    public InputPathEntry addDirectoryFromFileSystem(String originalInput, File base) {
        InputPathEntryImpl.Builder builder = new InputPathEntryImpl.Builder(originalInput);
        File file = new File("");
        try {
            MessageDigest md = computeHashes ? MessageDigest.getInstance("MD5") : null;
            AtomicInteger byteCount = new AtomicInteger();
            recursivelyAddFiles(base, file, md, byteCount);
            if (md != null) {
                String hash = new BigInteger(1, md.digest()).toString(16);
                builder.setHash(hash);
            }
            builder.setByteCount(byteCount.get());
        } catch (IOException | NoSuchAlgorithmException e) {
            LOGGER.error("Caught exception in addDirectoryFromFileSystem, {}", originalInput, e);
            builder.addException(e);
        }
        return builder.build();
    }

    private void recursivelyAddFiles(File baseDirectory,
                                     File dirRelativeToBase,
                                     MessageDigest md,
                                     AtomicInteger byteCount) throws IOException {
        File dir = new File(baseDirectory, dirRelativeToBase.getPath());
        if (dir.isDirectory()) {
            File[] subDirs = dir.listFiles(File::isDirectory);
            if (subDirs != null) {
                for (File subDir : subDirs) {
                    recursivelyAddFiles(baseDirectory, new File(dirRelativeToBase, subDir.getName()), md, byteCount);
                }
            }
            File[] files = dir.listFiles(f -> !f.isDirectory());
            if (files != null) {
                String pathString = dirRelativeToBase.getPath();
                String[] packageParts =
                        pathString.isEmpty() ? new String[0] :
                                (pathString.startsWith("/") ? pathString.substring(1) : pathString)
                                        .split("/");
                for (File file : files) {
                    String name = file.getName();
                    LOGGER.debug("File {} in path {}", name, String.join("/", packageParts));
                    data.add(Stream.concat(Arrays.stream(packageParts), Stream.of(name)).toArray(String[]::new),
                            file.toURI());
                    if (md != null) {
                        byte[] data = Files.readAllBytes(file.toPath());
                        byteCount.addAndGet(data.length);
                        md.digest(data);
                    } else {
                        byteCount.addAndGet((int) file.length());
                    }
                }
            }
        }
    }
}
