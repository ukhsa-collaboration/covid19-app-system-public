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
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload;
import uk.nhs.nhsx.keyfederation.download.DiagnosisKeysDownloadResponse;
import uk.nhs.nhsx.keyfederation.download.ExposureKeysPayload;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;

import static java.time.ZoneOffset.UTC;
import static java.util.stream.Collectors.toList;

public class FederatedKeyUploader {

    private static final Logger logger = LogManager.getLogger(DiagnosisKeysSubmissionService.class);

    private final S3Storage s3Storage;
    private final BucketName bucketName;
    private final String federatedKeySourcePrefix;
    private final Supplier<Instant> clock;
    private final Supplier<String> dateStringProvider;
    private final List<String> validOrigins;

    public FederatedKeyUploader(S3Storage s3Storage,
                                BucketName bucketName,
                                String federatedKeySourcePrefix,
                                Supplier<Instant> clock,
                                List<String> validOrigins) {
        this.s3Storage = s3Storage;
        this.bucketName = bucketName;
        this.federatedKeySourcePrefix = federatedKeySourcePrefix;
        this.clock = clock;
        this.dateStringProvider = () -> DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC).format(clock.get());
        this.validOrigins = validOrigins;
    }

    public void acceptKeysFromFederatedServer(DiagnosisKeysDownloadResponse payload) {
        indexStoreModelKeysPerOrigin(payload)
            .forEach((origin, keys) -> handleOriginKeys(payload, origin, keys));
    }

    public Map<String, List<StoredTemporaryExposureKey>> indexStoreModelKeysPerOrigin(DiagnosisKeysDownloadResponse payload) {
        var keysPerOrigin = new HashMap<String, List<StoredTemporaryExposureKey>>();
        payload.exposures.forEach(it -> {
            if (!keysPerOrigin.containsKey(it.origin)) {
                keysPerOrigin.put(it.origin, new ArrayList<>());
            }
            var keysList = keysPerOrigin.get(it.origin);
            var storedTemporaryExposureKey = new StoredTemporaryExposureKey(
                it.keyData, it.rollingStartNumber, it.rollingPeriod, it.transmissionRiskLevel
            );
            keysList.add(storedTemporaryExposureKey);
        });
        return keysPerOrigin;
    }

    private void handleOriginKeys(DiagnosisKeysDownloadResponse payload,
                                  String origin,
                                  List<StoredTemporaryExposureKey> temporaryExposureKeys) {
        var validKeys = temporaryExposureKeys.stream()
            .filter(this::isValidKey)
            .collect(toList());

        var invalidKeysCount = temporaryExposureKeys.size() - validKeys.size();

        logger.info(
            "Downloaded from federated server valid keys={}, invalid keys={}, origin={}",
            validKeys.size(), invalidKeysCount, origin
        );

        if (validOrigins.contains(origin)) {
            if (validKeys.size() > 0) {
                var exposureKeysPayload = new ExposureKeysPayload(origin, payload.batchTag, validKeys);

                uploadOriginKeysToS3(exposureKeysPayload);

                logger.debug("Stored in S3");
            } else {
                logger.info(
                    "Skip store to s3 because no valid keys were found or all keys were invalid, " +
                        "origin={}, batchTag={}", origin, payload.batchTag
                );
            }
        } else {
            logger.warn(
                "Skip store to s3 because origin is invalid, origin={}, batchTag={}",
                origin, payload.batchTag
            );
        }
    }

    private Boolean isValidKey(StoredTemporaryExposureKey temporaryExposureKey) {
        return Optional.ofNullable(temporaryExposureKey)
            .filter(tek -> isKeyValid(tek.key))
            .filter(tek -> isRollingStartNumberValid(clock, tek.rollingStartNumber, tek.rollingPeriod))
            .filter(tek -> isRollingPeriodValid(tek.rollingPeriod))
            .filter(tek -> isTransmissionRiskLevelValid(tek.transmissionRisk))
            .isPresent();
    }

    private boolean isKeyValid(String key) {
        var isValid = key != null && isBase64EncodedAndLessThan32Bytes(key);

        if (!isValid) {
            logger.debug("Key is invalid. Key={}", key);
        }

        return isValid;
    }

    public static boolean isRollingStartNumberValid(Supplier<Instant> clock, long rollingStartNumber, int rollingPeriod) {
        var now = clock.get();
        long TEN_MINUTES_INTERVAL_SECONDS = 600L;
        var currentInstant = now.getEpochSecond() / TEN_MINUTES_INTERVAL_SECONDS;
        var expiryPeriod = clock.get().minus(14, ChronoUnit.DAYS).getEpochSecond() / TEN_MINUTES_INTERVAL_SECONDS;
        var isValid = (rollingStartNumber + rollingPeriod) >= expiryPeriod &&
            rollingStartNumber <= currentInstant;

        if (!isValid) {
            logger.debug("Key is invalid. Now={}, rollingStartNumber={}, rollingPeriod={}", now, rollingStartNumber, rollingPeriod);
        }

        return isValid;
    }

    private boolean isRollingPeriodValid(int isRollingPeriod) {
        var isValid = isRollingPeriod > 0 && isRollingPeriod <= 144;

        if (!isValid) {
            logger.debug("Key is invalid. rollingPeriod={}", isRollingPeriod);
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

    private void uploadOriginKeysToS3(ExposureKeysPayload exposureKeysPayload) {
        var payload = new StoredTemporaryExposureKeyPayload(exposureKeysPayload.temporaryExposureKeys);
        var objectKey = ObjectKey.of(
            federatedKeySourcePrefix + "/" + exposureKeysPayload.origin + "/" +
            dateStringProvider.get() + "/" + exposureKeysPayload.batchTag + ".json"
        );
        s3Storage.upload(S3Storage.Locator.of(bucketName, objectKey), ContentType.APPLICATION_JSON, Sources.byteSourceFor(Jackson.toJson(payload)));
    }
}
