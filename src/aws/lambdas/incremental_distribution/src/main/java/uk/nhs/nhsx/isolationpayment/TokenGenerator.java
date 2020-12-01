package uk.nhs.nhsx.isolationpayment;

import java.security.SecureRandom;

public class TokenGenerator {

    /**
     * Generates unique ID from secure random source of 32 bytes with hex representation
     */
    public static String getToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return convertBytesToHex(bytes);
    }

    private static String convertBytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte byteValue : bytes) {
            result.append(String.format("%02x", byteValue));
        }
        return result.toString();
    }
}
