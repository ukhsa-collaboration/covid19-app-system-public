package uk.nhs.nhsx.diagnosiskeyssubmission

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.domain.TestKit

class TestKitAwareObjectKeyNameProviderTest {

    @ParameterizedTest
    @EnumSource(TestKit::class)
    fun `generates key for testkit`(testKit: TestKit) {
        val objectKey = TestKitAwareObjectKeyNameProvider({ ObjectKey.of("json-filename") }, testKit)
            .generateObjectKeyName()

        expectThat(objectKey)
            .isEqualTo(ObjectKey.of("mobile/${testKit}/json-filename"))
    }
}
