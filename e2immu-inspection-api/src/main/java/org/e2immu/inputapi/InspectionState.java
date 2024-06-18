package org.e2immu.inputapi;

import java.util.Objects;

public enum InspectionState {
    TRIGGER_BYTECODE_INSPECTION(1, Inspector.BYTE_CODE_INSPECTION),
    STARTING_BYTECODE(2, Inspector.BYTE_CODE_INSPECTION),
    FINISHED_BYTECODE(3, Inspector.BYTE_CODE_INSPECTION),
    INIT_JAVA_PARSER(4, Inspector.JAVA_PARSER_INSPECTION),
    TRIGGER_JAVA_PARSER(5, Inspector.JAVA_PARSER_INSPECTION),
    STARTING_JAVA_PARSER(6, Inspector.JAVA_PARSER_INSPECTION),
    FINISHED_JAVA_PARSER(7, Inspector.JAVA_PARSER_INSPECTION),
    BY_HAND_WITHOUT_STATEMENTS(8, Inspector.BY_HAND_WITHOUT_STATEMENTS),
    BY_HAND(8, Inspector.BY_HAND),
    BUILT(9, null);

    private final int state;
    private final Inspector inspector;

    InspectionState(int state, Inspector inspector) {
        this.state = state;
        this.inspector = inspector;
    }

    public boolean ge(InspectionState other) {
        return state >= other.state;
    }

    public boolean le(InspectionState other) {
        return state <= other.state;
    }

    public boolean lt(InspectionState other) {
        return state < other.state;
    }

    public Inspector getInspector() {
        return Objects.requireNonNull(inspector, "Need to query before the type is built!");
    }

    public boolean isDone() {
        return this == FINISHED_BYTECODE || this == FINISHED_JAVA_PARSER || this == BUILT;
    }
}
