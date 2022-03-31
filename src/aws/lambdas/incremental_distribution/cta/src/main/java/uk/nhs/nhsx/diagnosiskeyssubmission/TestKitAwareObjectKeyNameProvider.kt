package uk.nhs.nhsx.diagnosiskeyssubmission

import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider
import uk.nhs.nhsx.domain.TestKit

class TestKitAwareObjectKeyNameProvider(
    private val rootDelegate: ObjectKeyNameProvider,
    private val testKit: TestKit
) : ObjectKeyNameProvider {
    override fun generateObjectKeyName() =
        ObjectKey.of("""mobile/${testKit.name}/${rootDelegate.generateObjectKeyName()}""")
}
