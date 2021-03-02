package uk.nhs.nhsx.analyticssubmission.model;

import uk.nhs.nhsx.core.events.Events;

import static uk.nhs.nhsx.analyticssubmission.PostDistrictLaReplacer.replacePostDistrictLA;

public class StoredAnalyticsSubmissionPayload {

    //    Window
    public final String startDate;
    public final String endDate;

    //    Metadata
    public final String postalDistrict;
    public final String deviceModel;
    public final String operatingSystemVersion;
    public final String latestApplicationVersion;
    public final String localAuthority;

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
    public final Integer receivedVoidTestResultEnteredManually;
    public final Integer receivedPositiveTestResultEnteredManually;
    public final Integer receivedNegativeTestResultEnteredManually;
    public final Integer receivedVoidTestResultViaPolling;
    public final Integer receivedPositiveTestResultViaPolling;
    public final Integer receivedNegativeTestResultViaPolling;
    public final Integer hasSelfDiagnosedBackgroundTick;
    public final Integer hasTestedPositiveBackgroundTick;
    public final Integer isIsolatingForSelfDiagnosedBackgroundTick;
    public final Integer isIsolatingForTestedPositiveBackgroundTick;
    public final Integer isIsolatingForHadRiskyContactBackgroundTick;
    public final Integer receivedRiskyContactNotification;
    public final Integer startedIsolation;
    public final Integer receivedPositiveTestResultWhenIsolatingDueToRiskyContact;
    public final Integer receivedActiveIpcToken;
    public final Integer haveActiveIpcTokenBackgroundTick;
    public final Integer selectedIsolationPaymentsButton;
    public final Integer launchedIsolationPaymentsApplication;
    public final Integer receivedPositiveLFDTestResultViaPolling;
    public final Integer receivedNegativeLFDTestResultViaPolling;
    public final Integer receivedVoidLFDTestResultViaPolling;
    public final Integer receivedPositiveLFDTestResultEnteredManually;
    public final Integer receivedNegativeLFDTestResultEnteredManually;
    public final Integer receivedVoidLFDTestResultEnteredManually;
    public final Integer hasTestedLFDPositiveBackgroundTick;
    public final Integer isIsolatingForTestedLFDPositiveBackgroundTick;
    public final Integer totalExposureWindowsNotConsideredRisky;
    public final Integer totalExposureWindowsConsideredRisky;
    public final Integer acknowledgedStartOfIsolationDueToRiskyContact;
    public final Integer hasRiskyContactNotificationsEnabledBackgroundTick;
    public final Integer totalRiskyContactReminderNotifications;
    public final Integer receivedUnconfirmedPositiveTestResult;
    public final Integer isIsolatingForUnconfirmedTestBackgroundTick;
    public final Integer launchedTestOrdering;
    public final Integer didHaveSymptomsBeforeReceivedTestResult;
    public final Integer didRememberOnsetSymptomsDateBeforeReceivedTestResult;
    public final Integer didAskForSymptomsOnPositiveTestEntry;
    public final Integer declaredNegativeResultFromDCT;

    private StoredAnalyticsSubmissionPayload(String postalDistrict,
                                             String deviceModel,
                                             String operatingSystemVersion,
                                             String latestApplicationVersion,
                                             String localAuthority,
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
                                             boolean includesMultipleApplicationVersions,
                                             Integer receivedVoidTestResultEnteredManually,
                                             Integer receivedPositiveTestResultEnteredManually,
                                             Integer receivedNegativeTestResultEnteredManually,
                                             Integer receivedVoidTestResultViaPolling,
                                             Integer receivedPositiveTestResultViaPolling,
                                             Integer receivedNegativeTestResultViaPolling,
                                             Integer hasSelfDiagnosedBackgroundTick,
                                             Integer hasTestedPositiveBackgroundTick,
                                             Integer isIsolatingForSelfDiagnosedBackgroundTick,
                                             Integer isIsolatingForTestedPositiveBackgroundTick,
                                             Integer isIsolatingForHadRiskyContactBackgroundTick,
                                             Integer receivedRiskyContactNotification,
                                             Integer startedIsolation,
                                             Integer receivedPositiveTestResultWhenIsolatingDueToRiskyContact,
                                             Integer receivedActiveIpcToken,
                                             Integer haveActiveIpcTokenBackgroundTick,
                                             Integer selectedIsolationPaymentsButton,
                                             Integer launchedIsolationPaymentsApplication,
                                             Integer receivedPositiveLFDTestResultViaPolling,
                                             Integer receivedNegativeLFDTestResultViaPolling,
                                             Integer receivedVoidLFDTestResultViaPolling,
                                             Integer receivedPositiveLFDTestResultEnteredManually,
                                             Integer receivedNegativeLFDTestResultEnteredManually,
                                             Integer receivedVoidLFDTestResultEnteredManually,
                                             Integer hasTestedLFDPositiveBackgroundTick,
                                             Integer isIsolatingForTestedLFDPositiveBackgroundTick,
                                             Integer totalExposureWindowsNotConsideredRisky,
                                             Integer totalExposureWindowsConsideredRisky,
                                             Integer acknowledgedStartOfIsolationDueToRiskyContact,
                                             Integer hasRiskyContactNotificationsEnabledBackgroundTick,
                                             Integer totalRiskyContactReminderNotifications,
                                             Integer receivedUnconfirmedPositiveTestResult,
                                             Integer isIsolatingForUnconfirmedTestBackgroundTick,
                                             Integer launchedTestOrdering,
                                             Integer didHaveSymptomsBeforeReceivedTestResult,
                                             Integer didRememberOnsetSymptomsDateBeforeReceivedTestResult,
                                             Integer didAskForSymptomsOnPositiveTestEntry,
                                             Integer declaredNegativeResultFromDCT) {
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
        this.receivedVoidTestResultEnteredManually = receivedVoidTestResultEnteredManually;
        this.receivedPositiveTestResultEnteredManually = receivedPositiveTestResultEnteredManually;
        this.receivedNegativeTestResultEnteredManually = receivedNegativeTestResultEnteredManually;
        this.receivedVoidTestResultViaPolling = receivedVoidTestResultViaPolling;
        this.receivedPositiveTestResultViaPolling = receivedPositiveTestResultViaPolling;
        this.receivedNegativeTestResultViaPolling = receivedNegativeTestResultViaPolling;
        this.includesMultipleApplicationVersions = includesMultipleApplicationVersions;
        this.hasSelfDiagnosedBackgroundTick = hasSelfDiagnosedBackgroundTick;
        this.hasTestedPositiveBackgroundTick = hasTestedPositiveBackgroundTick;
        this.isIsolatingForSelfDiagnosedBackgroundTick = isIsolatingForSelfDiagnosedBackgroundTick;
        this.isIsolatingForTestedPositiveBackgroundTick = isIsolatingForTestedPositiveBackgroundTick;
        this.isIsolatingForHadRiskyContactBackgroundTick = isIsolatingForHadRiskyContactBackgroundTick;
        this.receivedRiskyContactNotification = receivedRiskyContactNotification;
        this.startedIsolation = startedIsolation;
        this.receivedPositiveTestResultWhenIsolatingDueToRiskyContact = receivedPositiveTestResultWhenIsolatingDueToRiskyContact;
        this.receivedActiveIpcToken = receivedActiveIpcToken;
        this.haveActiveIpcTokenBackgroundTick = haveActiveIpcTokenBackgroundTick;
        this.selectedIsolationPaymentsButton = selectedIsolationPaymentsButton;
        this.launchedIsolationPaymentsApplication = launchedIsolationPaymentsApplication;
        this.localAuthority = localAuthority;
        this.receivedPositiveLFDTestResultViaPolling = receivedPositiveLFDTestResultViaPolling;
        this.receivedNegativeLFDTestResultViaPolling = receivedNegativeLFDTestResultViaPolling;
        this.receivedVoidLFDTestResultViaPolling = receivedVoidLFDTestResultViaPolling;
        this.receivedPositiveLFDTestResultEnteredManually = receivedPositiveLFDTestResultEnteredManually;
        this.receivedNegativeLFDTestResultEnteredManually = receivedNegativeLFDTestResultEnteredManually;
        this.receivedVoidLFDTestResultEnteredManually = receivedVoidLFDTestResultEnteredManually;
        this.hasTestedLFDPositiveBackgroundTick = hasTestedLFDPositiveBackgroundTick;
        this.isIsolatingForTestedLFDPositiveBackgroundTick = isIsolatingForTestedLFDPositiveBackgroundTick;
        this.totalExposureWindowsNotConsideredRisky = totalExposureWindowsNotConsideredRisky;
        this.totalExposureWindowsConsideredRisky = totalExposureWindowsConsideredRisky;
        this.acknowledgedStartOfIsolationDueToRiskyContact = acknowledgedStartOfIsolationDueToRiskyContact;
        this.hasRiskyContactNotificationsEnabledBackgroundTick = hasRiskyContactNotificationsEnabledBackgroundTick;
        this.totalRiskyContactReminderNotifications = totalRiskyContactReminderNotifications;
        this.receivedUnconfirmedPositiveTestResult = receivedUnconfirmedPositiveTestResult;
        this.isIsolatingForUnconfirmedTestBackgroundTick = isIsolatingForUnconfirmedTestBackgroundTick;
        this.launchedTestOrdering = launchedTestOrdering;
        this.didHaveSymptomsBeforeReceivedTestResult = didHaveSymptomsBeforeReceivedTestResult;
        this.didRememberOnsetSymptomsDateBeforeReceivedTestResult = didRememberOnsetSymptomsDateBeforeReceivedTestResult;
        this.didAskForSymptomsOnPositiveTestEntry = didAskForSymptomsOnPositiveTestEntry;
        this.declaredNegativeResultFromDCT = declaredNegativeResultFromDCT;
    }

    public static StoredAnalyticsSubmissionPayload convertFrom(ClientAnalyticsSubmissionPayload clientPayload, Events events) {
        PostDistrictLADTuple postalDistrictLADTuple = replacePostDistrictLA(clientPayload.metadata.postalDistrict, clientPayload.metadata.localAuthority, events);
        return new StoredAnalyticsSubmissionPayload(
            postalDistrictLADTuple.postDistrict,
            clientPayload.metadata.deviceModel,
            clientPayload.metadata.operatingSystemVersion,
            clientPayload.metadata.latestApplicationVersion,
            postalDistrictLADTuple.localAuthorityId,
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
            clientPayload.includesMultipleApplicationVersions,
            clientPayload.metrics.receivedVoidTestResultEnteredManually,
            clientPayload.metrics.receivedPositiveTestResultEnteredManually,
            clientPayload.metrics.receivedNegativeTestResultEnteredManually,
            clientPayload.metrics.receivedVoidTestResultViaPolling,
            clientPayload.metrics.receivedPositiveTestResultViaPolling,
            clientPayload.metrics.receivedNegativeTestResultViaPolling,
            clientPayload.metrics.hasSelfDiagnosedBackgroundTick,
            clientPayload.metrics.hasTestedPositiveBackgroundTick,
            clientPayload.metrics.isIsolatingForSelfDiagnosedBackgroundTick,
            clientPayload.metrics.isIsolatingForTestedPositiveBackgroundTick,
            clientPayload.metrics.isIsolatingForHadRiskyContactBackgroundTick,
            clientPayload.metrics.receivedRiskyContactNotification,
            clientPayload.metrics.startedIsolation,
            clientPayload.metrics.receivedPositiveTestResultWhenIsolatingDueToRiskyContact,
            clientPayload.metrics.receivedActiveIpcToken,
            clientPayload.metrics.haveActiveIpcTokenBackgroundTick,
            clientPayload.metrics.selectedIsolationPaymentsButton,
            clientPayload.metrics.launchedIsolationPaymentsApplication,
            clientPayload.metrics.receivedPositiveLFDTestResultViaPolling,
            clientPayload.metrics.receivedNegativeLFDTestResultViaPolling,
            clientPayload.metrics.receivedVoidLFDTestResultViaPolling,
            clientPayload.metrics.receivedPositiveLFDTestResultEnteredManually,
            clientPayload.metrics.receivedNegativeLFDTestResultEnteredManually,
            clientPayload.metrics.receivedVoidLFDTestResultEnteredManually,
            clientPayload.metrics.hasTestedLFDPositiveBackgroundTick,
            clientPayload.metrics.isIsolatingForTestedLFDPositiveBackgroundTick,
            clientPayload.metrics.totalExposureWindowsNotConsideredRisky,
            clientPayload.metrics.totalExposureWindowsConsideredRisky,
            clientPayload.metrics.acknowledgedStartOfIsolationDueToRiskyContact,
            clientPayload.metrics.hasRiskyContactNotificationsEnabledBackgroundTick,
            clientPayload.metrics.totalRiskyContactReminderNotifications,
            clientPayload.metrics.receivedUnconfirmedPositiveTestResult,
            clientPayload.metrics.isIsolatingForUnconfirmedTestBackgroundTick,
            clientPayload.metrics.launchedTestOrdering,
            clientPayload.metrics.didHaveSymptomsBeforeReceivedTestResult,
            clientPayload.metrics.didRememberOnsetSymptomsDateBeforeReceivedTestResult,
            clientPayload.metrics.didAskForSymptomsOnPositiveTestEntry,
            clientPayload.metrics.declaredNegativeResultFromDCT
        );
    }
}
