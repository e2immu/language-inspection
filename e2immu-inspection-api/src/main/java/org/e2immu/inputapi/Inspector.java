package org.e2immu.inputapi;

public record Inspector(boolean statements, String label) {

    public static final Inspector BYTE_CODE_INSPECTION = new Inspector(false, "Byte code");
    public static final Inspector JAVA_PARSER_INSPECTION = new Inspector(true, "Java parser");

    public static final Inspector BY_HAND = new Inspector(true, "By hand");
    public static final Inspector BY_HAND_WITHOUT_STATEMENTS = new Inspector(false, "By hand");

}