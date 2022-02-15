@file:Suppress("unused")

package uk.nhs.nhsx.virology.domain

import uk.nhs.nhsx.virology.exchange.CtaExchangeResponseV2
import uk.nhs.nhsx.virology.lookup.VirologyLookupResponseV2

object TestInstructionsValidator {

    fun validateOrThrow(responseV2: VirologyLookupResponseV2) {
        validateOrThrow(responseV2.extract())
    }

    fun validateOrThrow(responseV2: CtaExchangeResponseV2) {
        validateOrThrow(responseV2.extract())
    }

    private fun validateOrThrow(testInstructions: TestInstructions) {
        with(testInstructions) {
            if (!requiresConfirmatoryTest && shouldOfferFollowUpTest) {
                error("requiresConfirmatoryTest/shouldOfferFollowUpTest illegal state")
            }

            if (!requiresConfirmatoryTest && confirmatoryDayLimit != null) {
                error("requiresConfirmatoryTest/confirmatoryDayLimit illegal state")
            }
        }
    }

    private data class TestInstructions(
        val requiresConfirmatoryTest: Boolean,
        val confirmatoryDayLimit: Int?,
        val shouldOfferFollowUpTest: Boolean,
    )

    private fun CtaExchangeResponseV2.extract() = TestInstructions(
        requiresConfirmatoryTest = requiresConfirmatoryTest,
        confirmatoryDayLimit = confirmatoryDayLimit,
        shouldOfferFollowUpTest = shouldOfferFollowUpTest,
    )

    private fun VirologyLookupResponseV2.extract() = TestInstructions(
        requiresConfirmatoryTest = requiresConfirmatoryTest,
        confirmatoryDayLimit = confirmatoryDayLimit,
        shouldOfferFollowUpTest = shouldOfferFollowUpTest,
    )
}
