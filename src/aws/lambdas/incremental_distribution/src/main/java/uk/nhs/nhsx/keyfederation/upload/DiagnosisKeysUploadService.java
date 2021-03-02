package uk.nhs.nhsx.keyfederation.upload;

import com.amazonaws.services.lambda.runtime.Context;
import uk.nhs.nhsx.core.events.Events;
import uk.nhs.nhsx.core.events.InfoEvent;
import uk.nhs.nhsx.diagnosiskeydist.Submission;
import uk.nhs.nhsx.diagnosiskeydist.SubmissionRepository;
import uk.nhs.nhsx.keyfederation.BatchTagService;
import uk.nhs.nhsx.keyfederation.InteropClient;
import uk.nhs.nhsx.keyfederation.UploadedDiagnosisKeys;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static uk.nhs.nhsx.core.Jackson.toJson;

public class DiagnosisKeysUploadService {
    private final static int NO_BATCH_SIZE_LIMIT_LEGACY_BATCH_SIZE = 0;

    private final InteropClient interopClient;
    private final SubmissionRepository submissionRepository;
    private final String region;
    private final BatchTagService batchTagService;
    private final boolean uploadRiskLevelDefaultEnabled;
    private final int uploadRiskLevelDefault;
    private final int initialUploadHistoryDays;
    private final int maxUploadBatchSize;
    private final int maxUploadBatchLimit;
    private final int maxSubsequentBatchUploadCount;
    private final Context context;
    private final Events events;
    private final Supplier<Instant> clock;

    public DiagnosisKeysUploadService(Supplier<Instant> clock,
                                      InteropClient interopClient,
                                      SubmissionRepository submissionRepository,
                                      BatchTagService batchTagService,
                                      String region,
                                      boolean uploadRiskLevelDefaultEnabled,
                                      int uploadRiskLevelDefault,
                                      int initialUploadHistoryDays,
                                      int maxUploadBatchSize,
                                      int maxSubsequentBatchUploadCount,
                                      Context context,
                                      Events events) {
        this.clock = clock;
        this.interopClient = interopClient;
        this.submissionRepository = submissionRepository;
        this.batchTagService = batchTagService;
        this.region = region;
        this.uploadRiskLevelDefaultEnabled = uploadRiskLevelDefaultEnabled;
        this.uploadRiskLevelDefault = uploadRiskLevelDefault;
        this.initialUploadHistoryDays = initialUploadHistoryDays;
        this.maxUploadBatchSize = maxUploadBatchSize;
        this.maxUploadBatchLimit = maxUploadBatchSize - 4; //4 mobile submissions/sec
        this.maxSubsequentBatchUploadCount = maxSubsequentBatchUploadCount;
        this.context = context;
        this.events = events;
    }

    public int loadKeysAndUploadToFederatedServer() throws Exception {
        int submissionCount = 0;
        long iterationDuration = 0L;
        Instant lastUploadedSubmissionTime = getLastUploadedTime();

        for (int i = 1; i <= maxSubsequentBatchUploadCount; i++) {
            var startTime = System.currentTimeMillis();
            var result = loadKeysAndUploadOneBatchToFederatedServer(lastUploadedSubmissionTime, i);

            submissionCount += result.submissionCount;

            if (maxUploadBatchSize == NO_BATCH_SIZE_LIMIT_LEGACY_BATCH_SIZE
                || lastUploadedSubmissionTime.equals(result.lastUploadedSubmissionTime)
                || result.submissionCount < maxUploadBatchLimit) {
                break;
            }

            lastUploadedSubmissionTime = result.lastUploadedSubmissionTime;
            iterationDuration = Math.max(iterationDuration, System.currentTimeMillis() - startTime);
            var remainingTimeInMillis = context.getRemainingTimeInMillis();
            if (iterationDuration >= remainingTimeInMillis) {
                break;
            }
        }

        return submissionCount;
    }

    static class BatchUploadResult {
        public final Instant lastUploadedSubmissionTime;
        public final int submissionCount;

        public BatchUploadResult(Instant lastUploadedSubmissionTime, int submissionCount) {
            this.lastUploadedSubmissionTime = lastUploadedSubmissionTime;
            this.submissionCount = submissionCount;
        }
    }

    public BatchUploadResult loadKeysAndUploadOneBatchToFederatedServer(Instant lastUploadedSubmissionTime, int batchNumber) throws Exception {

        events.emit(getClass(), new InfoEvent("Begin: Upload diagnosis keys to the Nearform server - batch " + batchNumber));

        List<Submission> newSubmissions = submissionRepository.loadAllSubmissions(
            lastUploadedSubmissionTime.toEpochMilli(),
            maxUploadBatchSize == NO_BATCH_SIZE_LIMIT_LEGACY_BATCH_SIZE ? Integer.MAX_VALUE : maxUploadBatchLimit,
            maxUploadBatchSize == NO_BATCH_SIZE_LIMIT_LEGACY_BATCH_SIZE ? Integer.MAX_VALUE : maxUploadBatchSize);

        List<ExposureUpload> exposureKeys = getUploadRequestRawPayload(newSubmissions);

        var transformedExposureKeys = exposureKeys.stream().map(this::updateRiskLevelIfDefaultEnabled).collect(Collectors.toList());

        events.emit(getClass(), new InfoEvent("Loading and transforming keys from submissions finished (from " +
            (newSubmissions.isEmpty() ? null : (newSubmissions.get(0).submissionDate)) +
            " to " + (newSubmissions.isEmpty() ? null : (newSubmissions.get(newSubmissions.size() - 1).submissionDate) )+
            "), keyCount=" + transformedExposureKeys.size() +
            " (batch " + batchNumber + ")"));

        if (!transformedExposureKeys.isEmpty()) {
            String payload = toJson(transformedExposureKeys);
            DiagnosisKeysUploadResponse uploadResponse = interopClient.uploadKeys(payload);
            if (uploadResponse != null) {
                events.emit(getClass(), new UploadedDiagnosisKeys(uploadResponse.insertedExposures, lastUploadedSubmissionTime, batchNumber));

                Instant updatedLastUploadedSubmissionTime = Instant.ofEpochMilli(newSubmissions.stream().map(submission -> submission.submissionDate.getTime()).max(Long::compare).orElseThrow());
                batchTagService.updateLastUploadState(updatedLastUploadedSubmissionTime.toEpochMilli() / 1000);

                return new BatchUploadResult(updatedLastUploadedSubmissionTime, newSubmissions.size());
            }
        } else {
            events.emit(getClass(), new InfoEvent("No keys were available for uploading to federation server with submission date greater than " +
                lastUploadedSubmissionTime +
                " -batch " + batchNumber));
        }

        return new BatchUploadResult(lastUploadedSubmissionTime, newSubmissions.size());
    }

    public ExposureUpload updateRiskLevelIfDefaultEnabled(ExposureUpload upload) {
        return new ExposureUpload(upload.keyData,
            upload.rollingStartNumber,
            uploadRiskLevelDefaultEnabled ? uploadRiskLevelDefault : upload.transmissionRiskLevel,
            upload.rollingPeriod,
            upload.regions);
    }

    public List<ExposureUpload> getUploadRequestRawPayload(List<Submission> submissions) // FIXME filter expired keys (rollingStartNumber & rollingPeriod)
    {
        List<ExposureUpload> exposures = new ArrayList<>();
        submissions
            .forEach(submission ->
                submission.payload.temporaryExposureKeys.forEach(exposure ->
                    exposures.add(
                        new ExposureUpload(
                            exposure.key,
                            exposure.rollingStartNumber,
                            exposure.transmissionRisk,
                            exposure.rollingPeriod,
                            List.of(this.region)
                        )
                    )));
        return exposures;
    }

    private Instant getLastUploadedTime() {
        return batchTagService.getLastUploadState()
            .map(it -> {
                events.emit(getClass(), new InfoEvent("Last uploaded timestamp from db " + it.lastUploadedTimeStamp));
                return Instant.ofEpochSecond(it.lastUploadedTimeStamp);
            })
            .orElse(
                clock.get().minus(Duration.ofDays(initialUploadHistoryDays))
            );
    }
}
