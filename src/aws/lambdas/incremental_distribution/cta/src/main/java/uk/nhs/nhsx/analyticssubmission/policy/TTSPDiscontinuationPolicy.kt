package uk.nhs.nhsx.analyticssubmission.policy

import uk.nhs.nhsx.analyticssubmission.model.ClientAnalyticsSubmissionPayload
import uk.nhs.nhsx.core.Environment
import java.time.LocalDate

class TTSPDiscontinuationPolicy(value: LocalDate) : GovernmentPolicy(value) {
    override fun scrub(payload: ClientAnalyticsSubmissionPayload) = payload
        .apply {
            with(metrics) {
                receivedActiveIpcToken = null
                haveActiveIpcTokenBackgroundTick = null
                selectedIsolationPaymentsButton = null
                launchedIsolationPaymentsApplication = null
            }
        }

    companion object {
        private val TTSP_DISCONTINUED_EFFECTIVE_DATE =
            Environment.EnvironmentKey.localDate("TTSP_DISCONTINUED_EFFECTIVE_DATE")

        val default: LocalDate = LocalDate.of(2022, 4, 7)

        fun from(e: Environment) = TTSPDiscontinuationPolicy(
            e.access.defaulted(TTSP_DISCONTINUED_EFFECTIVE_DATE) { default }
        )
    }
}
