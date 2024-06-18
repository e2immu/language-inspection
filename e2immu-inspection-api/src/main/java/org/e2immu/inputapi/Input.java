package org.e2immu.inputapi;

public interface Input {
    static boolean acceptFQN(String fqn) {
        return !fqn.startsWith("jdk.internal.");
    }
}
