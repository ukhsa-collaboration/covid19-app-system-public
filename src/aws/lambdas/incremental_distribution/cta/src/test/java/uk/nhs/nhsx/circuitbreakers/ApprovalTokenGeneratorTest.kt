package uk.nhs.nhsx.circuitbreakers

import org.junit.jupiter.api.RepeatedTest
import strikt.api.expectThat
import strikt.assertions.matches

class ApprovalTokenGeneratorTest {

    @RepeatedTest(100)
    fun `token contains only alphanumeric characters`() {
        expectThat(ApprovalTokenGenerator()).matches("[A-Za-z\\d]{50}".toRegex())
    }
}
