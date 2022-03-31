package uk.nhs.nhsx.analyticssubmission.policy

import uk.nhs.nhsx.core.Environment

data class PolicyConfig(
    val policies: List<GovernmentPolicy>
) {
    companion object {
        fun from(e: Environment) = PolicyConfig(
            policies = listOf(
                TTSPDiscontinuationPolicy.from(e)
            )
        )
    }
}
