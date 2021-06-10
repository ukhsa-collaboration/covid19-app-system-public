package uk.nhs.nhsx.virology.result

import uk.nhs.nhsx.core.exceptions.ApiResponseException
import uk.nhs.nhsx.core.exceptions.HttpStatusCode.UNPROCESSABLE_ENTITY_422
import uk.nhs.nhsx.domain.TestKit
import uk.nhs.nhsx.domain.TestKit.LAB_RESULT
import uk.nhs.nhsx.domain.TestKit.RAPID_RESULT
import uk.nhs.nhsx.domain.TestKit.RAPID_SELF_REPORTED
import uk.nhs.nhsx.domain.TestResult
import uk.nhs.nhsx.domain.TestResult.Plod
import uk.nhs.nhsx.domain.TestResult.Positive

object VirologyTestKitValidator {

    fun validate(testKit: TestKit, testResult: TestResult) {
        if (testResult == Plod && testKit != LAB_RESULT)
            throw ApiResponseException(UNPROCESSABLE_ENTITY_422, "validation error: Invalid test type value")

        if ((testKit == RAPID_RESULT || testKit == RAPID_SELF_REPORTED) && testResult != Positive)
            throw ApiResponseException(UNPROCESSABLE_ENTITY_422, "validation error: Invalid test type value")
    }
}
