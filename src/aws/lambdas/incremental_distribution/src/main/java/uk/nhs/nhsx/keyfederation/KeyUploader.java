package uk.nhs.nhsx.keyfederation;

import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.s3.ObjectKey;
import uk.nhs.nhsx.core.aws.s3.S3Storage;
import uk.nhs.nhsx.core.aws.s3.Sources;
import uk.nhs.nhsx.diagnosiskeyssubmission.DiagnosisKeysSubmissionService;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKey;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKeysPayload;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload;

import java.util.Base64;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;

public class KeyUploader {

    private static final Logger logger = LogManager.getLogger(DiagnosisKeysSubmissionService.class);

    private final S3Storage s3Storage;
    private final BucketName bucketName;
    private final String federatedKeyPrefix;

    public KeyUploader(S3Storage s3Storage, BucketName bucketName, String federatedKeyPrefix) {
        this.s3Storage = s3Storage;
        this.bucketName = bucketName;
        this.federatedKeyPrefix = federatedKeyPrefix;
    }

    public void acceptKeysFromFederatedServer(ClientTemporaryExposureKeysPayload payload, String batchTag){
        acceptTemporaryExposureKeys(payload, acceptedPayload -> uploadToS3(acceptedPayload,batchTag));
    }

    public void acceptTemporaryExposureKeys(ClientTemporaryExposureKeysPayload payload,
                                            Consumer<ClientTemporaryExposureKeysPayload> acceptPayload) {
        var validKeys = payload.temporaryExposureKeys.stream()
            .filter(this::isValidKey)
            .collect(toList());

        var invalidKeysCount = payload.temporaryExposureKeys.size() - validKeys.size();

        logger.info(
            "Downloaded from federated server valid keys={}, invalid keys={}",
            validKeys.size(), invalidKeysCount
        );

        if (invalidKeysCount == 0)
            acceptPayload.accept(payload);
    }

    private Boolean isValidKey(ClientTemporaryExposureKey temporaryExposureKey) {
        return Optional.ofNullable(temporaryExposureKey)
            .filter(tek -> isKeyValid(tek.key))
            .filter(tek -> isRollingStartNumberValid(tek.rollingStartNumber))
            .filter(tek -> isRollingPeriodValid(tek.rollingPeriod))
            .isPresent();
    }

    private boolean isKeyValid(String key) {
        return key != null && isBase64EncodedAndLessThan32Bytes(key);
    }

    private boolean isRollingStartNumberValid(int rollingStartNumber) {
        return rollingStartNumber >= 0;
    }

    private boolean isRollingPeriodValid(int isRollingPeriod) {
        return isRollingPeriod == 144;
    }

    private boolean isBase64EncodedAndLessThan32Bytes(String value) {
        try {
            byte[] key = Base64.getDecoder().decode(value);
            return key.length < 32;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void uploadToS3(ClientTemporaryExposureKeysPayload payload, String batchTag) {
        StoredTemporaryExposureKeyPayload uploadPayload = convertToStoredModel(payload);
        ObjectKey objectKey = ObjectKey.of( federatedKeyPrefix + "_" + batchTag + ".json");
        s3Storage.upload(S3Storage.Locator.of(bucketName, objectKey), ContentType.APPLICATION_JSON, Sources.byteSourceFor(Jackson.toJson(uploadPayload)));
    }

    // FIXME Move StoredTemporaryExposureKey to common area
    private StoredTemporaryExposureKeyPayload convertToStoredModel(ClientTemporaryExposureKeysPayload payload) {
        return new StoredTemporaryExposureKeyPayload(
            payload.temporaryExposureKeys
                .stream()
                .map(tek -> new StoredTemporaryExposureKey(tek.key, tek.rollingStartNumber, tek.rollingPeriod, tek.transmissionRiskLevel))
                .collect(toList())
        );
    }
}
