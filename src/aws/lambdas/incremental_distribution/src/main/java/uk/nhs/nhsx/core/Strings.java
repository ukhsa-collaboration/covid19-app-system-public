package uk.nhs.nhsx.core;

public final class Strings {

    private Strings() {
        /* private constructor */
    }

    public static boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }
}
