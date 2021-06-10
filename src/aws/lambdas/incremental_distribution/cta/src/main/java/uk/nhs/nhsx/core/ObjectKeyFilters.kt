package uk.nhs.nhsx.core

import uk.nhs.nhsx.core.aws.s3.ObjectKey
import java.util.function.Predicate

object ObjectKeyFilters {
    fun federated(): FederatedObjectKeyFilters = FederatedObjectKeyFilters()

    fun batched(): BatchObjectKeyFilters = BatchObjectKeyFilters()

    // mobile keys are at root level
    private val isMobileKey: Predicate<ObjectKey> = Predicate { !it.value.contains("/") }
    private val isMobileLabResultKey: Predicate<ObjectKey> = Predicate { it.value.startsWith("mobile/LAB_RESULT/") }
    private val isMobileRapidResultKey: Predicate<ObjectKey> = Predicate { it.value.startsWith("mobile/RAPID_RESULT/") }
    private val isMobileRapidSelfReportedResultKey: Predicate<ObjectKey> = Predicate { it.value.startsWith("mobile/RAPID_SELF_REPORTED/") }

    private fun isWhitelistedFederatedKey(allowedPrefixes: List<String>): Predicate<ObjectKey> =
        Predicate { allowedPrefixes.any(it.value::startsWith) }

    class BatchObjectKeyFilters {
        fun withPrefixes(allowedPrefixes: List<String>): Predicate<ObjectKey> = isMobileKey
            .or(isMobileLabResultKey)
            .or(isMobileRapidResultKey)
            .or(isMobileRapidSelfReportedResultKey)
            .or(isWhitelistedFederatedKey(allowedPrefixes))
    }

    class FederatedObjectKeyFilters {
        fun withPrefixes(allowedPrefixes: List<String>): Predicate<ObjectKey> = isMobileKey
            .or(isMobileLabResultKey)
            .or(isMobileRapidResultKey)
            .or(isMobileRapidSelfReportedResultKey)
            .or(isWhitelistedFederatedKey(allowedPrefixes))
    }
}
