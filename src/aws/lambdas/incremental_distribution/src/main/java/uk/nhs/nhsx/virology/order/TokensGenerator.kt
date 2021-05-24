package uk.nhs.nhsx.virology.order

import uk.nhs.nhsx.core.random.crockford.CrockfordDammRandomStringGenerator
import uk.nhs.nhsx.domain.CtaToken.Companion.of
import uk.nhs.nhsx.domain.DiagnosisKeySubmissionToken
import uk.nhs.nhsx.domain.TestResultPollingToken
import uk.nhs.nhsx.virology.persistence.TestOrder
import java.time.LocalDateTime
import java.util.*

object TokensGenerator {
    private val ctaTokenGenerator = CrockfordDammRandomStringGenerator()

    fun generateVirologyTokens() = TestOrder(
        of(ctaTokenGenerator.generate()),
        TestResultPollingToken.of(UUID.randomUUID().toString()),
        DiagnosisKeySubmissionToken.of(UUID.randomUUID().toString()),
        LocalDateTime.now().plusWeeks(4) // placeholder, not used when persisting to table
    )
}
