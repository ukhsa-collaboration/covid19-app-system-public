package uk.nhs.nhsx.keyfederation;

import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.core.aws.s3.*;
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
    private final List<String> validRegions;

    public FederatedKeyUploader(S3Storage s3Storage, BucketName bucketName, String federatedKeySourcePrefix, Supplier<Instant> clock, List<String> validRegions) {
        this.s3Storage = s3Storage;
        this.bucketName = bucketName;
        this.federatedKeySourcePrefix = federatedKeySourcePrefix;
        this.clock = clock;
        this.dateStringProvider = () -> DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC).format(clock.get());
        this.validRegions = validRegions;
    }

    public void acceptKeysFromFederatedServer(DiagnosisKeysDownloadResponse payload) {
        Map<String, List<StoredTemporaryExposureKey>> storedKeyPayloadsMap = convertToStoredModel(payload);
        storedKeyPayloadsMap.forEach((region, temporaryExposureKeys) -> {
            var validKeys = temporaryExposureKeys.stream()
                .filter(this::isValidKey)
                .collect(toList());

            var invalidKeysCount = temporaryExposureKeys.size() - validKeys.size();

            logger.info(
                "Downloaded from federated server valid keys={}, invalid keys={},region = {}",
                validKeys.size(), invalidKeysCount, region
            );

            if (validRegions.contains(region)) {
                if (validKeys.size() > 0) {
                    ExposureKeysPayload exposureKeysPayload = new ExposureKeysPayload(region, payload.batchTag, validKeys);
                    uploadToS3(exposureKeysPayload);
                } else {
                    logger.info("Skip store to s3 because no valid keys were found or all keys were invalid, region={}, batchTag={}", region, payload.batchTag);
                }
            } else {
                logger.info("Skip store to s3 because region is invalid, region={}, batchTag={}", region, payload.batchTag);
            }

        });
    }

    private Boolean isValidKey(StoredTemporaryExposureKey temporaryExposureKey) {
        return Optional.ofNullable(temporaryExposureKey)
            .filter(tek -> isKeyValid(tek.key))
            .filter(tek -> isRollingStartNumberValid(tek.rollingStartNumber))
            .filter(tek -> isRollingPeriodValid(tek.rollingPeriod))
            .filter(tek -> isTransmissionRiskLevelValid(tek.transmissionRisk))
            .isPresent();
    }

    private boolean isKeyValid(String key) {
        return key != null && isBase64EncodedAndLessThan32Bytes(key);
    }

    private boolean isRollingStartNumberValid(long rollingStartNumber) {
        long currentInstant = LocalDateTime.ofInstant(clock.get(), UTC).toEpochSecond(UTC) / 600L;
        long expiryPeriod = LocalDateTime.ofInstant(clock.get().minus(14, ChronoUnit.DAYS), UTC).toEpochSecond(UTC) / 600L;
        return rollingStartNumber >= 0 &&
            rollingStartNumber >= expiryPeriod &&
            rollingStartNumber < currentInstant;
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

    private boolean isTransmissionRiskLevelValid(int transmissionRiskLevel) {
        return transmissionRiskLevel >= 0 && transmissionRiskLevel <= 7;
    }

    private void uploadToS3(ExposureKeysPayload payload) {
        uploadKeysForRegion(payload.region, payload.temporaryExposureKeys, payload.batchTag);
    }

    private void uploadKeysForRegion(String region, List<StoredTemporaryExposureKey> keys, String batchTag) {
        StoredTemporaryExposureKeyPayload payload = new StoredTemporaryExposureKeyPayload(keys);
        ObjectKey objectKey = ObjectKey.of(federatedKeySourcePrefix + "/" + region + "/" + dateStringProvider.get() + "/" + batchTag + ".json");
        s3Storage.upload(S3Storage.Locator.of(bucketName, objectKey), ContentType.APPLICATION_JSON, Sources.byteSourceFor(Jackson.toJson(payload)));
    }

    public Map<String, List<StoredTemporaryExposureKey>> convertToStoredModel(DiagnosisKeysDownloadResponse payload) {
        Map<String, List<StoredTemporaryExposureKey>> responsesMap = new HashMap<>();
        payload.exposures.forEach(
            exposure -> {
                String region = exposure.regions.get(0);
                if (!responsesMap.containsKey(region)) {
                    responsesMap.put(region, new ArrayList<>());
                }
                List<StoredTemporaryExposureKey> responses = responsesMap.get(region);
                StoredTemporaryExposureKey storedTemporaryExposureKey = new StoredTemporaryExposureKey(exposure.keyData, exposure.rollingStartNumber, exposure.rollingPeriod, exposure.transmissionRiskLevel);
                responses.add(storedTemporaryExposureKey);
            }
        );
        return responsesMap;
    }
}
