package uk.nhs.nhsx.core;

import java.util.List;
import java.util.function.Predicate;

public class ObjectKeyFilter {

    public static Predicate<String> includeMobileAndAllowedPrefixes(List<String> allowedPrefixes) {
        Predicate<String> isMobileKey = includeMobileKeysOnly();
        Predicate<String> isWhitelistedFederatedKey = objectKey -> allowedPrefixes.stream().anyMatch(objectKey::startsWith);
        return isMobileKey.or(isWhitelistedFederatedKey);
    }

    private static Predicate<String> includeMobileKeysOnly() {
        return objectKey -> !objectKey.contains("/"); // mobile keys are at root level
    }
}
