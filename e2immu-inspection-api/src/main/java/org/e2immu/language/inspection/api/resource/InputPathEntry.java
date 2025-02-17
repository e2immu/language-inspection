package org.e2immu.language.inspection.api.resource;

import java.net.URI;
import java.util.List;
import java.util.Set;

public interface InputPathEntry {

    /**
     * input
     *
     * @return String path as given in InputConfiguration
     */
    String path();

    /**
     * @return URI as computed from path()
     */
    URI uri();

    /**
     * Optionally computed. Purpose is to detect changes to the contents of the path entry.
     *
     * @return hash as string
     */
    String hash();

    /**
     * Computed at load-time.
     */
    int typeCount();

    /**
     * Computed at load-type.
     */
    int byteCount();

    Set<String> packages();

    /**
     *
     * @return not null when something went wrong.
     */
    List<Exception> exceptions();

    InputPathEntry withByteCount(int byteCount);

    InputPathEntry withException(Exception exception);

    InputPathEntry withHash(String hash);
}
