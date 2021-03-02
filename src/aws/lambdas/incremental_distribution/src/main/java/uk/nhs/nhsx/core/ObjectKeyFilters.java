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
            return isMobileKey()
                .or(isMobileLabResultKey())
                .or(isMobileRapidResultKey())
                .or(isMobileRapidSelfReportedResultKey())
                .or(isWhitelistedFederatedKey(allowedPrefixes));
        }
    }

    public static class FederatedObjectKeyFilters {
        public Predicate<ObjectKey> withPrefixes(List<String> allowedPrefixes) {
            return isMobileKey()
                .or(isMobileLabResultKey())
                .or(isWhitelistedFederatedKey(allowedPrefixes));
        }
    }

    private static Predicate<ObjectKey> isMobileKey() {
        return objectKey -> !objectKey.value.contains("/"); // mobile keys are at root level
    }

    private static Predicate<ObjectKey> isMobileLabResultKey() {
        return objectKey -> objectKey.value.startsWith("mobile/LAB_RESULT/");
    }

    private static Predicate<ObjectKey> isMobileRapidResultKey() {
        return objectKey -> objectKey.value.startsWith("mobile/RAPID_RESULT/");
    }

    private static Predicate<ObjectKey> isMobileRapidSelfReportedResultKey() {
        return objectKey -> objectKey.value.startsWith("mobile/RAPID_SELF_REPORTED/");
    }

    private static Predicate<ObjectKey> isWhitelistedFederatedKey(List<String> allowedPrefixes) {
        return objectKey -> allowedPrefixes.stream().anyMatch(objectKey.value::startsWith);
    }
}
