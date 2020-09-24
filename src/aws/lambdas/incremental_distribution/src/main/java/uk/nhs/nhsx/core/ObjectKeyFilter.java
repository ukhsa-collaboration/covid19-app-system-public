package uk.nhs.nhsx.core;

import java.util.function.Predicate;

public class ObjectKeyFilter {

    public static Predicate<String> excludeKeyWithPrefix(String prefix) {
        return objectKey -> !objectKey.startsWith(prefix);
    }

}
