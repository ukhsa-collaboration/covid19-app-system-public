package uk.nhs.nhsx.keyfederation.upload;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.core.SystemClock;
import uk.nhs.nhsx.diagnosiskeydist.Submission;
import uk.nhs.nhsx.diagnosiskeydist.SubmissionRepository;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey;
import uk.nhs.nhsx.keyfederation.BatchTagService;
import uk.nhs.nhsx.keyfederation.Exposure;
import uk.nhs.nhsx.keyfederation.InteropClient;

import static uk.nhs.nhsx.core.Jackson.toJson;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class DiagnosisKeysUploadService {

    private static final Logger logger = LogManager.getLogger(DiagnosisKeysUploadService.class);

    private final InteropClient interopClient;

    private final SubmissionRepository submissionRepository;

    private final String region;

    private final BatchTagService batchTagService;

    public DiagnosisKeysUploadService(InteropClient interopClient, SubmissionRepository submissionRepository, BatchTagService batchTagService,String region) {
        this.interopClient = interopClient;
        this.submissionRepository = submissionRepository;
        this.batchTagService = batchTagService;
        this.region = region;
    }


    public void uploadRequest() throws Exception {

        logger.info("Begin: Upload diagnosis keys to the Nearform server");

        Instant lastUploadedTime = getLastUploadedTime();
        Instant currentTime = SystemClock.CLOCK.get();
        List<Submission> allSubmissions = submissionRepository.loadAllSubmissions();
        List<Exposure> exposureKeys = getUploadRequestRawPayload(allSubmissions, lastUploadedTime, currentTime);

        logger.info("Loading keys from submissions finished, keyCount={}, earliestKeyTime={}, latestKeyTime={}",
            exposureKeys.size(), lastUploadedTime, currentTime);

        if (!exposureKeys.isEmpty()) {
            String payload = toJson(exposureKeys);
            DiagnosisKeysUploadResponse uploadResponse = interopClient.uploadKeys(payload);
            if (uploadResponse != null) {
                logger.info(String.format("Uploaded %s keys with submission date greater than %s to federation server",
                    uploadResponse.insertedExposures,
                    lastUploadedTime.toString()));
            }
            batchTagService.updateLastUploadState(currentTime.getEpochSecond());
        } else {
            logger.info("No keys were available for uploading to federation server");
        }
    }

    public List<Exposure> getUploadRequestRawPayload(List<Submission> submissions, Instant lastUploadedTime, Instant currentTime) {

        List<Exposure> exposures = new ArrayList<>();
        submissions.stream()
            .filter(submission -> isKeyValid(submission.submissionDate, lastUploadedTime, currentTime))
            .forEach(submission -> {
                List<StoredTemporaryExposureKey> temporaryExposureKeys = submission.payload.temporaryExposureKeys;
                temporaryExposureKeys.stream().forEach(exposure -> {
                    exposures.add(new Exposure(exposure.key,
                        exposure.rollingStartNumber,
                        exposure.transmissionRisk,
                        exposure.rollingPeriod,
                        List.of(this.region)
                    ));
                });
            });
        return exposures;
    }

    private Instant getLastUploadedTime() {
        return batchTagService.getLastUploadState().map(it -> {
            logger.info("Last uploaded timestamp from db {}", it.lastUploadedTimeStamp);
            return Instant.ofEpochSecond(it.lastUploadedTimeStamp);
        }).orElse(
            OffsetDateTime.now(ZoneOffset.UTC).minusDays(14).toInstant()
        );
    }


    private boolean isKeyValid(Date submissionDate, Instant from, Instant to) {
        Instant submissionDateInstant = Optional.ofNullable(submissionDate).map(Date::toInstant).orElse(Instant.MIN);
        return (submissionDateInstant.isAfter(from) &&
            submissionDateInstant.isBefore(to)) || (submissionDateInstant.equals(to));
    }

}
