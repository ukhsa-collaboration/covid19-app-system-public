package uk.nhs.nhsx.core.aws.ssm;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import com.google.common.base.Suppliers;
import uk.nhs.nhsx.core.signature.KeyId;

import java.util.function.Supplier;

public class AwsSsmFetcher implements KeyLookup {

    // cannot construct in class, as tests fail, but not sure how often this is constructed, so compromise here
    private static final Supplier<AWSSimpleSystemsManagement> ssmClient = Suppliers.memoize(AWSSimpleSystemsManagementClientBuilder::defaultClient);

    private final Supplier<KeyId> keyId;

    public AwsSsmFetcher(String ssmParameterName) {
        keyId = Suppliers.memoize(() -> loadKmsKeyId(ssmParameterName));
    }

    public KeyId getKmsKeyId() {
        return keyId.get();
    }

    private KeyId loadKmsKeyId(String ssmKeyIdParameterName) {
        GetParameterRequest request = new GetParameterRequest()
            .withName(ssmKeyIdParameterName);
        GetParameterResult result = ssmClient.get().getParameter(request);
        Parameter keyIDParameter = result.getParameter();

        return KeyId.of(keyIDParameter.getValue());
    }
}
