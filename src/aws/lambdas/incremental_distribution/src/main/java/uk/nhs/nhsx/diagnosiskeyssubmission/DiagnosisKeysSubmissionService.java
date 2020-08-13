package uk.nhs.nhsx.diagnosiskeyssubmission;

import com.amazonaws.services.dynamodbv2.document.Item;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.aws.dynamodb.AwsDynamoClient;
import uk.nhs.nhsx.core.aws.s3.*;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKey;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKeysPayload;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class DiagnosisKeysSubmissionService {

    private static final Logger logger = LoggerFactory.getLogger(DiagnosisKeysSubmissionService.class);

    private final BucketName bucketName;
    private final S3Storage s3Storage;
    private final AwsDynamoClient awsDynamoClient;
    private final ObjectKeyNameProvider objectKeyNameProvider;
    private final String tableName;
    private final String submissionTokensHashKey = "diagnosisKeySubmissionToken";

    public DiagnosisKeysSubmissionService(S3Storage s3Storage,
                                          AwsDynamoClient awsDynamoClient,
                                          ObjectKeyNameProvider objectKeyNameProvider,
                                          String tableName, BucketName name) {
        this.bucketName = name;
        this.s3Storage = s3Storage;
        this.awsDynamoClient = awsDynamoClient;
        this.objectKeyNameProvider = objectKeyNameProvider;
        this.tableName = tableName;
    }

    public void acceptTemporaryExposureKeys(ClientTemporaryExposureKeysPayload payload) {
        allValidMaybe(payload)
            .ifPresent(this::acceptPayload);
    }

    private Optional<ClientTemporaryExposureKeysPayload> allValidMaybe(ClientTemporaryExposureKeysPayload payload) {
        if (payload.temporaryExposureKeys.stream().allMatch(this::isValidKey))
            return Optional.of(payload);

        logger.warn("At least one key is invalid");

        return Optional.empty();
    }

    private Boolean isValidKey(ClientTemporaryExposureKey temporaryExposureKey) {
        return Optional.ofNullable(temporaryExposureKey)
            .filter(tek -> isKeyValid(tek.key))
            .filter(tek -> isRollingStartNumberValid(tek.rollingStartNumber))
            .filter(tek -> isRollingPeriodValid(tek.rollingPeriod))
            .isPresent();
    }

    private boolean isKeyValid(String key) {
        return key != null && isBase64Encoded(key);
    }

    private boolean isRollingStartNumberValid(int rollingStartNumber) {
        return rollingStartNumber >= 0;
    }

    private boolean isRollingPeriodValid(int isRollingPeriod) {
        return isRollingPeriod == 144;
    }

    private boolean isBase64Encoded(String value) {
        try {
            Base64.getDecoder().decode(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void acceptPayload(ClientTemporaryExposureKeysPayload payload) {
        matchDiagnosisToken(payload.diagnosisKeySubmissionToken)
            .ifPresent(item -> storeKeysAndDeleteToken(payload));
    }

    private Optional<Item> matchDiagnosisToken(UUID token) {
        Item item = awsDynamoClient.getItem(
            tableName,
            submissionTokensHashKey,
            token.toString()
        );

        if (item == null) logger.warn("Skipping, token {} not found", token);

        return Optional.ofNullable(item);
    }

    private void storeKeysAndDeleteToken(ClientTemporaryExposureKeysPayload payload) {
        uploadToS3(payload);
        deleteToken(payload.diagnosisKeySubmissionToken);
    }

    private void uploadToS3(ClientTemporaryExposureKeysPayload payload) {
        StoredTemporaryExposureKeyPayload uploadPayload = convertToStoredModel(payload);
        ObjectKey objectKey = objectKeyNameProvider.generateObjectKeyName().append(".json");
        s3Storage.upload(S3Storage.Locator.of(bucketName, objectKey), ContentType.APPLICATION_JSON, Sources.byteSourceFor(Jackson.toJson(uploadPayload)));
    }

    private StoredTemporaryExposureKeyPayload convertToStoredModel(ClientTemporaryExposureKeysPayload payload) {
        return new StoredTemporaryExposureKeyPayload(
            payload.temporaryExposureKeys
                .stream()
                .map(tek -> new StoredTemporaryExposureKey(tek.key, tek.rollingStartNumber, tek.rollingPeriod))
                .collect(Collectors.toList())
        );
    }

    private void deleteToken(UUID diagnosisKeySubmissionToken) {
        awsDynamoClient.deleteItem(
            tableName,
            submissionTokensHashKey,
            diagnosisKeySubmissionToken.toString()
        );
    }
}
