package uk.nhs.nhsx.diagnosiskeyssubmission;

import com.amazonaws.services.dynamodbv2.document.Item;
import org.apache.http.entity.ContentType;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.aws.dynamodb.AwsDynamoClient;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.s3.ByteArraySource;
import uk.nhs.nhsx.core.aws.s3.Locator;
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider;
import uk.nhs.nhsx.core.aws.s3.S3Storage;
import uk.nhs.nhsx.core.events.Events;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKey;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKeysPayload;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload;
import uk.nhs.nhsx.keyfederation.InvalidRollingPeriod;
import uk.nhs.nhsx.keyfederation.InvalidRollingStartNumber;
import uk.nhs.nhsx.keyfederation.InvalidTemporaryExposureKey;
import uk.nhs.nhsx.keyfederation.InvalidTransmissionRiskLevel;
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

    private static final String SUBMISSION_TOKENS_HASH_KEY = "diagnosisKeySubmissionToken";
    private static final int MAX_KEYS = 14;

    private final BucketName bucketName;
    private final S3Storage s3Storage;
    private final AwsDynamoClient awsDynamoClient;
    private final ObjectKeyNameProvider objectKeyNameProvider;
    private final String tableName;
    private final Supplier<Instant> clock;
    private final Events events;

    public DiagnosisKeysSubmissionService(S3Storage s3Storage,
                                          AwsDynamoClient awsDynamoClient,
                                          ObjectKeyNameProvider objectKeyNameProvider,
                                          String tableName,
                                          BucketName name,
                                          Supplier<Instant> clock,
                                          Events events) {
        this.bucketName = name;
        this.s3Storage = s3Storage;
        this.awsDynamoClient = awsDynamoClient;
        this.objectKeyNameProvider = objectKeyNameProvider;
        this.tableName = tableName;
        this.clock = clock;
        this.events = events;
    }

    public void acceptTemporaryExposureKeys(ClientTemporaryExposureKeysPayload payload) {
        allValidMaybe(payload)
            .ifPresent(this::acceptPayload);
    }

    private Optional<ClientTemporaryExposureKeysPayload> allValidMaybe(ClientTemporaryExposureKeysPayload payload) {
        if (payload.temporaryExposureKeys.size() > MAX_KEYS) {
            events.emit(getClass(), new TemporaryExposureKeysSubmissionOverflow(payload.temporaryExposureKeys.size(), MAX_KEYS));
            return Optional.empty();
        }

        var validKeys = payload.temporaryExposureKeys.stream().filter(this::isValidKey).collect(toList());
        var invalidKeysCount = payload.temporaryExposureKeys.size() - validKeys.size();

        events.emit(getClass(), new DownloadedTemporaryExposureKeys(validKeys.size(), invalidKeysCount));

        if (validKeys.size() > 0) {
            return Optional.of(new ClientTemporaryExposureKeysPayload(payload.diagnosisKeySubmissionToken, validKeys));
        }

        events.emit(getClass(), new EmptyTemporaryExposureKeys());
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
        if (!isValid) events.emit(getClass(), new InvalidTemporaryExposureKey(key));
        return isValid;
    }

    private boolean isRollingStartNumberValid(long rollingStartNumber, int rollingPeriod) {
        var now = clock.get();
        long TEN_MINUTES_INTERVAL_SECONDS = 600L;
        var currentInstant = now.getEpochSecond() / TEN_MINUTES_INTERVAL_SECONDS;
        var expiryPeriod = clock.get().minus(14, ChronoUnit.DAYS).getEpochSecond() / TEN_MINUTES_INTERVAL_SECONDS;
        var isValid = (rollingStartNumber + rollingPeriod) >= expiryPeriod && rollingStartNumber <= currentInstant;
        if (!isValid) {
            events.emit(getClass(), new InvalidRollingStartNumber(now, rollingStartNumber, rollingPeriod));
        }
        return isValid;
    }

    private boolean isRollingPeriodValid(int rollingPeriod) {
        boolean isValid = rollingPeriod > 0 && rollingPeriod <= 144;
        if(!isValid)  events.emit(getClass(), new InvalidRollingPeriod(rollingPeriod));
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
        boolean isValid = transmissionRiskLevel >= 0 && transmissionRiskLevel <= 7;
        if(!isValid)  events.emit(getClass(), new InvalidTransmissionRiskLevel(transmissionRiskLevel));
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
            SUBMISSION_TOKENS_HASH_KEY,
            token.toString()
        );

        if (item == null) {
            events.emit(getClass(), new DiagnosisTokenNotFound(token));
        }

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
            ByteArraySource.fromUtf8String(Jackson.toJson(uploadPayload))
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
            SUBMISSION_TOKENS_HASH_KEY,
            diagnosisKeySubmissionToken.toString()
        );
    }
}
