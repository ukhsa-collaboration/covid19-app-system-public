package uk.nhs.nhsx.diagnosiskeyssubmission;

import uk.nhs.nhsx.core.aws.s3.ObjectKey;
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider;
import uk.nhs.nhsx.virology.TestKit;

public class TestKitAwareObjectKeyNameProvider implements ObjectKeyNameProvider {

    private final ObjectKeyNameProvider rootDelegate;
    private final TestKit testKit;

    public TestKitAwareObjectKeyNameProvider(ObjectKeyNameProvider rootDelegate,
                                             TestKit testKit) {
        this.rootDelegate = rootDelegate;
        this.testKit = testKit;
    }

    @Override
    public ObjectKey generateObjectKeyName() {
        return ObjectKey.of("mobile/" + testKit.name() + "/" + rootDelegate.generateObjectKeyName());
    }
}
