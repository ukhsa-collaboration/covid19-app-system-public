package uk.nhs.nhsx.core.aws.s3;

public class FederatedKeysObjectKeyNameProvider implements ObjectKeyNameProvider{

    private final String federatedKeyPrefix;
    private final String batchTag;

    public FederatedKeysObjectKeyNameProvider(String federatedKeyPrefix, String batchTag) {
        this.federatedKeyPrefix = federatedKeyPrefix;
        this.batchTag = batchTag;
    }

    @Override
    public ObjectKey generateObjectKeyName() {
        return ObjectKey.of( federatedKeyPrefix + "_" + batchTag);
    }
}
