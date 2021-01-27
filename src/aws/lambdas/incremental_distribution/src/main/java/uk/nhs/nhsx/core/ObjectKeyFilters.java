package uk.nhs.nhsx.core;

import uk.nhs.nhsx.core.aws.s3.ObjectKey;

import java.util.List;
import java.util.function.Predicate;

public final class ObjectKeyFilters {

    private ObjectKeyFilters() {
    }

    public static FederatedObjectKeyFilters federated() {
        return new FederatedObjectKeyFilters();
    }

    public static BatchObjectKeyFilters batched() {
        return new BatchObjectKeyFilters();
    }

    public static class BatchObjectKeyFilters {
        public Predicate<ObjectKey> withPrefixes(List<String> allowedPrefixes) {
            Predicate<ObjectKey> isMobileKey = includeMobileKeysOnly();
            Predicate<ObjectKey> isMobileLabResultKey = includeMobileLabResultKeysOnly();
            Predicate<ObjectKey> isMobileRapidResultKey = includeMobileRapidResultKeysOnly();
            Predicate<ObjectKey> isWhitelistedFederatedKey = objectKey -> allowedPrefixes.stream().anyMatch(objectKey.value::startsWith);
            return isMobileKey.or(isMobileLabResultKey).or(isMobileRapidResultKey).or(isWhitelistedFederatedKey);
        }

        private static Predicate<ObjectKey> includeMobileKeysOnly() {
            return objectKey -> !objectKey.value.contains("/"); // mobile keys are at root level
        }

        private static Predicate<ObjectKey> includeMobileLabResultKeysOnly() {
            return objectKey -> objectKey.value.startsWith("mobile/LAB_RESULT/");
        }

        private static Predicate<ObjectKey> includeMobileRapidResultKeysOnly() {
            return objectKey -> objectKey.value.startsWith("mobile/RAPID_RESULT/");
        }
    }

    public static class FederatedObjectKeyFilters {
        public Predicate<ObjectKey> withPrefixes(List<String> allowedPrefixes) {
            Predicate<ObjectKey> isMobileKey = includeMobileKeysOnly();
            Predicate<ObjectKey> isMobileLabResultKey = includeMobileLabResultKeysOnly();
            Predicate<ObjectKey> isWhitelistedFederatedKey = objectKey -> allowedPrefixes.stream().anyMatch(objectKey.value::startsWith);
            return isMobileKey.or(isMobileLabResultKey).or(isWhitelistedFederatedKey);
        }

        private static Predicate<ObjectKey> includeMobileKeysOnly() {
            return objectKey -> !objectKey.value.contains("/"); // mobile keys are at root level
        }

        private static Predicate<ObjectKey> includeMobileLabResultKeysOnly() {
            return objectKey -> objectKey.value.startsWith("mobile/LAB_RESULT/");
        }
    }
}
