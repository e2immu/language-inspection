package org.e2immu.language.inspection.resource;

import org.e2immu.language.cst.api.element.FingerPrint;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.inspection.api.resource.ClassPathPart;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Set;

public class ClassPathPartImpl extends SourceSetImpl implements ClassPathPart {
    private final boolean runtimeOnly;
    private final URI uri;

    public ClassPathPartImpl(String name, Path path, Charset encoding, boolean test, boolean library,
                             boolean externalLibrary, boolean partOfJdk, Set<String> excludePackages,
                             Set<SourceSet> dependencies, URI uri, boolean runtimeOnly) {
        super(name, path, encoding, test, library, externalLibrary, partOfJdk, excludePackages, dependencies);
        this.runtimeOnly = runtimeOnly;
        this.uri = uri;
    }

    @Override
    public URI uri() {
        return uri;
    }

    @Override
    public boolean runtimeOnly() {
        return runtimeOnly;
    }
}
