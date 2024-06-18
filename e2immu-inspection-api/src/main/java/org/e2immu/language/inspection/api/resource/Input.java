package org.e2immu.language.inspection.api.resource;

public interface Input {
    static boolean acceptFQN(String fqn) {
        return !fqn.startsWith("jdk.internal.");
    }
}
