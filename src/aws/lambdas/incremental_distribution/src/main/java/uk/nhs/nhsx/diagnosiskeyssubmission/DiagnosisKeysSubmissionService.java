package uk.nhs.nhsx.diagnosiskeyssubmission;

import com.amazonaws.services.dynamodbv2.document.Item;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.aws.dynamodb.AwsDynamoClient;
import uk.nhs.nhsx.core.aws.s3.*;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKey;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKeysPayload;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload;
import uk.nhs.nhsx.virology.TestKit;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;

public class DiagnosisKeysSubmissionService {

    private static final Logger logger = LogManager.getLogger(DiagnosisKeysSubmissionService.class);

    private final BucketName bucketName;
    private final S3Storage s3Storage;
    private final AwsDynamoClient awsDynamoClient;
    private final ObjectKeyNameProvider objectKeyNameProvider;
    private final String tableName;
    private final String submissionTokensHashKey = "diagnosisKeySubmissionToken";
    private final Supplier<Instant> clock;

    public DiagnosisKeysSubmissionService(S3Storage s3Storage,
                                          AwsDynamoClient awsDynamoClient,
                                          ObjectKeyNameProvider objectKeyNameProvider,
                                          String tableName,
                                          BucketName name,
                                          Supplier<Instant> clock) {
        this.bucketName = name;
        this.s3Storage = s3Storage;
        this.awsDynamoClient = awsDynamoClient;
        this.objectKeyNameProvider = objectKeyNameProvider;
        this.tableName = tableName;
        this.clock = clock;
    }

    public void acceptTemporaryExposureKeys(ClientTemporaryExposureKeysPayload payload) {
        allValidMaybe(payload)
            .ifPresent(this::acceptPayload);
    }

    private Optional<ClientTemporaryExposureKeysPayload> allValidMaybe(ClientTemporaryExposureKeysPayload payload) {
        if (payload.temporaryExposureKeys.size() > 14) {
            logger.warn("Submission contains more than 14 keys");
            return Optional.empty();
        }

        var validKeys = payload.temporaryExposureKeys.stream().filter(this::isValidKey).collect(toList());
        var invalidKeysCount = payload.temporaryExposureKeys.size() - validKeys.size();

        if (validKeys.size() > 0) {
            if (invalidKeysCount > 0)
                logger.warn(
                    "Downloaded from mobile valid keys={}, invalid keys={}",
                    validKeys.size(), invalidKeysCount
                );
            return Optional.of(new ClientTemporaryExposureKeysPayload(payload.diagnosisKeySubmissionToken, validKeys));
        }

        logger.warn("Submission contains no key or is empty");
        return Optional.empty();
    }

    private Boolean isValidKey(ClientTemporaryExposureKey temporaryExposureKey) {
        return Optional.ofNullable(temporaryExposureKey)
            .filter(tek -> isKeyValid(tek.key))
            .filter(tek -> isRollingStartNumberValid(tek.rollingStartNumber, tek.rollingPeriod))
            .filter(tek -> isRollingPeriodValid(tek.rollingPeriod))
            .filter(tek -> isTransmissionRiskLevelValid(tek.transmissionRiskLevel))
            .isPresent();
    }

    private boolean isKeyValid(String key) {
        var isValid = key != null && isBase64EncodedAndLessThan32Bytes(key);

        if (!isValid) {
            logger.info("Key is invalid. Key={}", key);
        }
        return isValid;
    }

    private boolean isRollingStartNumberValid(long rollingStartNumber, int rollingPeriod) {
        var now = clock.get();
        long TEN_MINUTES_INTERVAL_SECONDS = 600L;
        var currentInstant = now.getEpochSecond() / TEN_MINUTES_INTERVAL_SECONDS;
        var expiryPeriod = clock.get().minus(14, ChronoUnit.DAYS).getEpochSecond() / TEN_MINUTES_INTERVAL_SECONDS;
        var isValid = (rollingStartNumber + rollingPeriod) >= expiryPeriod &&
            rollingStartNumber <= currentInstant;
        if (!isValid) {
            logger.debug("Key is invalid. Now={}, rollingStartNumber={}, rollingPeriod={}",
                now, rollingStartNumber, rollingPeriod);
        }
        return isValid;
    }

    private boolean isRollingPeriodValid(int rollingPeriod) {
        var isValid = rollingPeriod > 0 && rollingPeriod <= 144;

        if (!isValid) {
            logger.debug("Key is invalid. rollingPeriod={}", rollingPeriod);
        }
        return isValid;
    }

    private boolean isBase64EncodedAndLessThan32Bytes(String value) {
        try {
            byte[] key = Base64.getDecoder().decode(value);
            return key.length < 32;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isTransmissionRiskLevelValid(int transmissionRiskLevel) {
        var isValid = transmissionRiskLevel >= 0 && transmissionRiskLevel <= 7;

        if (!isValid) {
            logger.debug("Key is invalid. transmissionRiskLevel={}", transmissionRiskLevel);
        }
        return isValid;
    }

    private void acceptPayload(ClientTemporaryExposureKeysPayload payload) {
        matchDiagnosisToken(payload.diagnosisKeySubmissionToken)
            .map(this::testkitFrom)
            .ifPresent(it -> storeKeysAndDeleteToken(it, payload));
    }

    private TestKit testkitFrom(Item item) {
        return Optional.ofNullable(item.get("testKit"))
            .map(Object::toString)
            .map(TestKit::valueOf)
            .orElse(TestKit.LAB_RESULT);
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

    private void storeKeysAndDeleteToken(TestKit testKit, ClientTemporaryExposureKeysPayload payload) {
        uploadToS3(testKit, payload);
        deleteToken(payload.diagnosisKeySubmissionToken);
    }

    private void uploadToS3(TestKit testKit, ClientTemporaryExposureKeysPayload payload) {
        var uploadPayload = convertToStoredModel(payload);
        var provider = new TestKitAwareObjectKeyNameProvider(objectKeyNameProvider, testKit);
        var objectKey = provider.generateObjectKeyName().append(".json");

        s3Storage.upload(
            Locator.of(bucketName, objectKey),
            ContentType.APPLICATION_JSON,
            Sources.byteSourceFor(Jackson.toJson(uploadPayload))
        );
    }

    private StoredTemporaryExposureKeyPayload convertToStoredModel(ClientTemporaryExposureKeysPayload payload) {
        return new StoredTemporaryExposureKeyPayload(
            payload.temporaryExposureKeys
                .stream()
                .map(buildStoredTemporaryExposureKey())
                .collect(toList())
        );
    }

    private Function<ClientTemporaryExposureKey, StoredTemporaryExposureKey> buildStoredTemporaryExposureKey() {
        return (ClientTemporaryExposureKey tek) -> new StoredTemporaryExposureKey
            (tek.key, tek.rollingStartNumber, tek.rollingPeriod, tek.transmissionRiskLevel, tek.daysSinceOnsetOfSymptoms);
    }

    private void deleteToken(UUID diagnosisKeySubmissionToken) {
        awsDynamoClient.deleteItem(
            tableName,
            submissionTokensHashKey,
            diagnosisKeySubmissionToken.toString()
        );
    }
}
