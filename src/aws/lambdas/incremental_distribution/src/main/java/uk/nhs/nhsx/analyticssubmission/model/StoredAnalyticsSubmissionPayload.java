package uk.nhs.nhsx.analyticssubmission.model;

import static uk.nhs.nhsx.analyticssubmission.PostCodeDeserializer.mergeSmallPostcodes;

public class StoredAnalyticsSubmissionPayload {

    //    Window
    public final String startDate;
    public final String endDate;

    //    Metadata
    public final String postalDistrict;
    public final String deviceModel;
    public final String operatingSystemVersion;
    public final String latestApplicationVersion;

    //    Metrics
    public final Long cumulativeDownloadBytes;
    public final Long cumulativeUploadBytes;
    public final Long cumulativeCellularDownloadBytes;
    public final Long cumulativeCellularUploadBytes;
    public final Long cumulativeWifiDownloadBytes;
    public final Long cumulativeWifiUploadBytes;
    public final int checkedIn;
    public final int canceledCheckIn;
    public final int receivedVoidTestResult;
    public final int isIsolatingBackgroundTick;
    public final int hasHadRiskyContactBackgroundTick;
    public final int receivedPositiveTestResult;
    public final int receivedNegativeTestResult;
    public final int hasSelfDiagnosedPositiveBackgroundTick;
    public final int completedQuestionnaireAndStartedIsolation;
    public final int encounterDetectionPausedBackgroundTick;
    public final int completedQuestionnaireButDidNotStartIsolation;
    public final int totalBackgroundTasks;
    public final int runningNormallyBackgroundTick;
    public final int completedOnboarding;

    public final boolean includesMultipleApplicationVersions;

    private StoredAnalyticsSubmissionPayload(String postalDistrict,
                                             String deviceModel,
                                             String operatingSystemVersion,
                                             String latestApplicationVersion,
                                             Long cumulativeDownloadBytes,
                                             Long cumulativeUploadBytes,
                                             Long cumulativeCellularDownloadBytes,
                                             Long cumulativeCellularUploadBytes,
                                             Long cumulativeWifiDownloadBytes,
                                             Long cumulativeWifiUploadBytes,
                                             int checkedIn,
                                             int canceledCheckIn,
                                             int receivedVoidTestResult,
                                             int isIsolatingBackgroundTick,
                                             int hasHadRiskyContactBackgroundTick,
                                             int receivedPositiveTestResult,
                                             int receivedNegativeTestResult,
                                             int hasSelfDiagnosedPositiveBackgroundTick,
                                             int completedQuestionnaireAndStartedIsolation,
                                             int encounterDetectionPausedBackgroundTick,
                                             int completedQuestionnaireButDidNotStartIsolation,
                                             int totalBackgroundTasks,
                                             int runningNormallyBackgroundTick,
                                             int completedOnboarding,
                                             String startDate,
                                             String endDate,
                                             boolean includesMultipleApplicationVersions) {
        this.postalDistrict = postalDistrict;
        this.deviceModel = deviceModel;
        this.operatingSystemVersion = operatingSystemVersion;
        this.latestApplicationVersion = latestApplicationVersion;
        this.cumulativeDownloadBytes = cumulativeDownloadBytes;
        this.cumulativeUploadBytes = cumulativeUploadBytes;
        this.cumulativeCellularDownloadBytes = cumulativeCellularDownloadBytes;
        this.cumulativeCellularUploadBytes = cumulativeCellularUploadBytes;
        this.cumulativeWifiDownloadBytes = cumulativeWifiDownloadBytes;
        this.cumulativeWifiUploadBytes = cumulativeWifiUploadBytes;
        this.checkedIn = checkedIn;
        this.canceledCheckIn = canceledCheckIn;
        this.receivedVoidTestResult = receivedVoidTestResult;
        this.isIsolatingBackgroundTick = isIsolatingBackgroundTick;
        this.hasHadRiskyContactBackgroundTick = hasHadRiskyContactBackgroundTick;
        this.receivedPositiveTestResult = receivedPositiveTestResult;
        this.receivedNegativeTestResult = receivedNegativeTestResult;
        this.hasSelfDiagnosedPositiveBackgroundTick = hasSelfDiagnosedPositiveBackgroundTick;
        this.completedQuestionnaireAndStartedIsolation = completedQuestionnaireAndStartedIsolation;
        this.encounterDetectionPausedBackgroundTick = encounterDetectionPausedBackgroundTick;
        this.completedQuestionnaireButDidNotStartIsolation = completedQuestionnaireButDidNotStartIsolation;
        this.totalBackgroundTasks = totalBackgroundTasks;
        this.runningNormallyBackgroundTick = runningNormallyBackgroundTick;
        this.completedOnboarding = completedOnboarding;
        this.startDate = startDate;
        this.endDate = endDate;
        this.includesMultipleApplicationVersions = includesMultipleApplicationVersions;
    }

    public static StoredAnalyticsSubmissionPayload convertFrom(ClientAnalyticsSubmissionPayload clientPayload) {
        return new StoredAnalyticsSubmissionPayload(
                mergeSmallPostcodes(clientPayload.metadata.postalDistrict),
                clientPayload.metadata.deviceModel,
                clientPayload.metadata.operatingSystemVersion,
                clientPayload.metadata.latestApplicationVersion,
                clientPayload.metrics.cumulativeDownloadBytes,
                clientPayload.metrics.cumulativeUploadBytes,
                clientPayload.metrics.cumulativeCellularDownloadBytes,
                clientPayload.metrics.cumulativeCellularUploadBytes,
                clientPayload.metrics.cumulativeWifiDownloadBytes,
                clientPayload.metrics.cumulativeWifiUploadBytes,
                clientPayload.metrics.checkedIn,
                clientPayload.metrics.canceledCheckIn,
                clientPayload.metrics.receivedVoidTestResult,
                clientPayload.metrics.isIsolatingBackgroundTick,
                clientPayload.metrics.hasHadRiskyContactBackgroundTick,
                clientPayload.metrics.receivedPositiveTestResult,
                clientPayload.metrics.receivedNegativeTestResult,
                clientPayload.metrics.hasSelfDiagnosedPositiveBackgroundTick,
                clientPayload.metrics.completedQuestionnaireAndStartedIsolation,
                clientPayload.metrics.encounterDetectionPausedBackgroundTick,
                clientPayload.metrics.completedQuestionnaireButDidNotStartIsolation,
                clientPayload.metrics.totalBackgroundTasks,
                clientPayload.metrics.runningNormallyBackgroundTick,
                clientPayload.metrics.completedOnboarding,
                clientPayload.analyticsWindow.startDate,
                clientPayload.analyticsWindow.endDate,
                clientPayload.includesMultipleApplicationVersions
        );
    }
}
