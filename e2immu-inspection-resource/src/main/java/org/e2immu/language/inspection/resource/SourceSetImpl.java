package org.e2immu.language.inspection.resource;

import org.e2immu.language.cst.api.element.FingerPrint;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.support.SetOnce;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

public class SourceSetImpl implements SourceSet {
    private final String name;
    private final Path path;
    private final Charset encoding;
    private final boolean test;
    private final boolean library;
    private final boolean externalLibrary;
    private final boolean partOfJdk;
    private final Set<String> restrictToPackages;
    private final Set<SourceSet> dependencies;
    private final SetOnce<FingerPrint> fingerPrint = new SetOnce<>();
    private final SetOnce<FingerPrint> analysisFingerPrint = new SetOnce<>();

    public SourceSetImpl(String name, Path path,
                         Charset encoding,
                         boolean test, boolean library, boolean externalLibrary, boolean partOfJdk,
                         Set<String> restrictToPackages,
                         Set<SourceSet> dependencies) {
        this.name = name;
        this.path = path;
        this.encoding = encoding;
        this.test = test;
        this.library = library;
        this.externalLibrary = externalLibrary;
        this.partOfJdk = partOfJdk;
        this.restrictToPackages = restrictToPackages;
        this.dependencies = dependencies;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof SourceSetImpl sourceSet)) return false;
        return Objects.equals(name, sourceSet.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        String code = partOfJdk ? "[jdk]" : externalLibrary ? "[external]" : library ? "[library]" : test ? "[test]" : "";
        String pathString = path.toString();
        return name + code + (pathString.equals(name) ? "" : ":" + pathString);
    }

    @Override
    public Charset encoding() {
        return encoding;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Path path() {
        return path;
    }

    @Override
    public boolean test() {
        return test;
    }

    @Override
    public boolean library() {
        return library;
    }

    @Override
    public boolean externalLibrary() {
        return externalLibrary;
    }

    @Override
    public boolean partOfJdk() {
        return partOfJdk;
    }

    @Override
    public Set<String> restrictToPackages() {
        return restrictToPackages;
    }

    @Override
    public Set<SourceSet> dependencies() {
        return dependencies;
    }

    @Override
    public FingerPrint fingerPrint() {
        return fingerPrint.get();
    }

    @Override
    public void setFingerPrint(FingerPrint fingerPrint) {
        this.fingerPrint.set(fingerPrint);
    }

    @Override
    public FingerPrint analysisFingerPrintOrNull() {
        return analysisFingerPrint.getOrDefaultNull();
    }

    @Override
    public void setAnalysisFingerPrint(FingerPrint fingerPrint) {
        analysisFingerPrint.set(fingerPrint);
    }

    @Override
    public boolean acceptSource(String packageName, String typeName) {
        if (restrictToPackages.isEmpty()) return true;
        for (String packageString : restrictToPackages) {
            if (packageString.endsWith(".")) {
                if (packageName.startsWith(packageString) ||
                    packageName.equals(packageString.substring(0, packageString.length() - 1))) {
                    return true;
                }
            } else if (packageName.equals(packageString) || packageString.equals(packageName + "." + typeName)) {
                return true;
            }
        }
        return false;
    }
}
