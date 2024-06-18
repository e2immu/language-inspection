package org.e2immu.language.inspection.api;

public interface Input {
    static boolean acceptFQN(String fqn) {
        return !fqn.startsWith("jdk.internal.");
    }
}
