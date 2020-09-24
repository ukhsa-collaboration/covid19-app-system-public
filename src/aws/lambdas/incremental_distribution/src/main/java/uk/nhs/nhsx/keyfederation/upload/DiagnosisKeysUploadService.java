package uk.nhs.nhsx.keyfederation.upload;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.nhs.nhsx.diagnosiskeydist.Submission;
import uk.nhs.nhsx.diagnosiskeydist.SubmissionRepository;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey;
import uk.nhs.nhsx.keyfederation.BatchTagService;
import uk.nhs.nhsx.keyfederation.Exposure;
import uk.nhs.nhsx.keyfederation.InteropClient;
import uk.nhs.nhsx.keyfederation.upload.lookup.UploadKeysResult;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static uk.nhs.nhsx.core.Jackson.toJson;

public class DiagnosisKeysUploadService {

    private static final Logger logger = LogManager.getLogger(DiagnosisKeysUploadService.class);

    private final InteropClient interopClient;

    private final SubmissionRepository submissionRepository;

    private static final String REGION = "GB-EAW";

    private final BatchTagService batchTagService;

    private static final TimeZone TIME_ZONE_UTC = TimeZone.getTimeZone("UTC");

    public DiagnosisKeysUploadService(InteropClient interopClient, SubmissionRepository submissionRepository, BatchTagService batchTagService) {
        this.interopClient = interopClient;
        this.submissionRepository = submissionRepository;
        this.batchTagService = batchTagService;
    }


    public void uploadRequest() throws Exception {

        logger.info("Begin: Upload diagnosis keys to the Nearform server");

        Date lastUploadedTime = getLastUploadedTime();
        Date currentTime = getCurrentTime();
        List<Submission> allSubmissions = submissionRepository.loadAllSubmissions();
        List<Exposure> exposureKeys = getUploadRequestRawPayload(allSubmissions,lastUploadedTime,currentTime);

        if(!exposureKeys.isEmpty()){
            String payload = toJson(exposureKeys);
            DiagnosisKeysUploadResponse uploadResponse = interopClient.uploadKeys(payload);
            if (uploadResponse != null) {
                logger.info(String.format("Uploaded %s keys with submission date greater than %s to federation server",
                    uploadResponse.insertedExposures,
                    formatDate(lastUploadedTime)));
            }
            batchTagService.updateLastUploadState(formatDate(currentTime));
        }
    }

    public List<Exposure> getUploadRequestRawPayload(List<Submission> submissions, Date lastUploadedTime, Date currentTime) {

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
                        List.of(REGION)
                    ));
                });
            });
        return exposures;
    }

    private Date getLastUploadedTime() {
        return batchTagService.getLastUploadState().map(it -> {
            logger.info("Last uploaded timeStamp from db "+it.lastUploadedTimeStamp);

            return parse(String.valueOf(it.lastUploadedTimeStamp));
        }).orElse(
            Date.from(LocalDateTime.now().minusDays(14).toInstant(ZoneOffset.UTC))
        );
    }

    private Date getCurrentTime(){
        return Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC));
    }



    private static Date parse(String date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        simpleDateFormat.setTimeZone(TIME_ZONE_UTC);

        try {
            return simpleDateFormat.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new Date();
    }

    private String formatDate(Date date){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        return sdf.format(date);
    }

    private boolean isKeyValid(Date submissionDate, Date from, Date to) {
        return (submissionDate.after(from) &&
            submissionDate.before(to)) || (submissionDate.equals(to));
    }

}
