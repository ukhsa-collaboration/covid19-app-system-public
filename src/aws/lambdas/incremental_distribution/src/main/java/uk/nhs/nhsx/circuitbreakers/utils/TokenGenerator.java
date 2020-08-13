package uk.nhs.nhsx.circuitbreakers.utils;

import java.security.SecureRandom;

public final class TokenGenerator {

    /**
     *  Generates alphanumeric token of 50 characters
     */
    public static String getToken() {
        int startIndex = 48; // numeral '0'
        int endIndex = 122; // letter 'z'
        int limit = 50;
        SecureRandom random = new SecureRandom();

        return random.ints(startIndex, endIndex + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(limit)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
