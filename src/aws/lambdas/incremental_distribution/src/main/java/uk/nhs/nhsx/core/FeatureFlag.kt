package uk.nhs.nhsx.core

fun interface FeatureFlag {
    fun isEnabled(): Boolean
}
