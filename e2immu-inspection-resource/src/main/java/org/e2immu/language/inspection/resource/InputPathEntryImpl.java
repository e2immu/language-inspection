package org.e2immu.language.inspection.resource;

import org.e2immu.language.inspection.api.resource.InputPathEntry;

import java.net.URI;
import java.util.*;
import java.util.stream.Stream;

public record InputPathEntryImpl(String path,
                                 URI uri,
                                 String hash,
                                 Set<String> packages,
                                 int typeCount,
                                 int byteCount,
                                 List<Exception> exceptions) implements InputPathEntry {

    public static class Builder {
        private final String path;
        private URI uri;
        private String hash;
        private final Set<String> packages = new HashSet<>();
        private int typeCount;
        private int byteCount;
        private final List<Exception> exceptions = new ArrayList<>();

        public Builder(String path) {
            this.path = path;
        }

        public String path() {
            return path;
        }

        public URI uri() {
            return uri;
        }

        public Builder setHash(String hash) {
            this.hash = hash;
            return this;
        }

        public Builder setTypeCount(int typeCount) {
            this.typeCount = typeCount;
            return this;
        }

        public Builder setByteCount(int byteCount) {
            this.byteCount = byteCount;
            return this;
        }

        public Builder setURI(URI uri) {
            this.uri = uri;
            return this;
        }

        public Builder addException(Exception exception) {
            this.exceptions.add(exception);
            return this;
        }

        public Builder addPackage(String packageName) {
            this.packages.add(packageName);
            return this;
        }

        public InputPathEntry build() {
            return new InputPathEntryImpl(path, uri, hash, Set.copyOf(packages), typeCount, byteCount,
                    List.copyOf(exceptions));
        }

        @Override
        public String toString() {
            return "Builder{" +
                   "path='" + path + '\'' +
                   ", uri=" + uri +
                   ", hash='" + hash + '\'' +
                   ", packages=" + packages +
                   ", typeCount=" + typeCount +
                   ", byteCount=" + byteCount +
                   ", exceptions=" + exceptions +
                   '}';
        }
    }

    @Override
    public InputPathEntry withByteCount(int byteCount) {
        return new InputPathEntryImpl(path, uri, hash, packages, typeCount, byteCount, exceptions);
    }

    @Override
    public InputPathEntry withException(Exception exception) {
        return new InputPathEntryImpl(path, uri, hash, packages, typeCount, byteCount,
                Stream.concat(exceptions.stream(), Stream.of(exception)).toList());
    }

    @Override
    public InputPathEntry withHash(String hash) {
        return new InputPathEntryImpl(path, uri, hash, packages, typeCount, byteCount, exceptions);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof InputPathEntryImpl pathEntry)) return false;
        return Objects.equals(uri(), pathEntry.uri());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uri());
    }
}
