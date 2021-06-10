package uk.nhs.nhsx.diagnosiskeyssubmission

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.domain.TestKit

class TestKitAwareObjectKeyNameProviderTest {

    @Test
    fun `generates key for testkit`() {
        TestKit.values().forEach {
            val provider = TestKitAwareObjectKeyNameProvider({ ObjectKey.of("json-filename") }, it)
            assertThat(provider.generateObjectKeyName(), equalTo(ObjectKey.of("mobile/${it.name}/json-filename")))
        }
    }
}
