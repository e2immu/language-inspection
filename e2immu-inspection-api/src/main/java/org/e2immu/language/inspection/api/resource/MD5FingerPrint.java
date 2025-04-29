package org.e2immu.language.inspection.api.resource;

import org.e2immu.language.cst.api.element.FingerPrint;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

public class MD5FingerPrint implements FingerPrint {

    public static final FingerPrint NO_FINGERPRINT = new MD5FingerPrint(new byte[]{});

    private final byte[] bytes;

    public MD5FingerPrint(byte[] bytes) {
        this.bytes = bytes;
    }

    public static FingerPrint compute(String sourceCode) {
        return compute(sourceCode.getBytes());
    }

    public static FingerPrint compute(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(bytes);
            byte[] digest = md.digest();
            return new MD5FingerPrint(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static FingerPrint compute(MessageDigest md, byte[] bytes) {
        md.reset();
        byte[] digest = md.digest(bytes);
        return new MD5FingerPrint(digest);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof MD5FingerPrint that)) return false;
        return Objects.deepEquals(bytes, that.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public String toString() {
        if (this == NO_FINGERPRINT) return "<no fingerprint>";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] arr = md.digest(bytes);
            return Base64.getEncoder().encodeToString(arr);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
