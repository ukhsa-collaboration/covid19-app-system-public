package uk.nhs.nhsx.analyticssubmission.model;

public class AnalyticsMetrics {

    public Long cumulativeDownloadBytes;
    public Long cumulativeUploadBytes;
    public Long cumulativeCellularDownloadBytes;
    public Long cumulativeCellularUploadBytes;
    public Long cumulativeWifiDownloadBytes;
    public Long cumulativeWifiUploadBytes;
    public int checkedIn;
    public int canceledCheckIn;
    public int receivedVoidTestResult;
    public int isIsolatingBackgroundTick;
    public int hasHadRiskyContactBackgroundTick;
    public int receivedPositiveTestResult;
    public int receivedNegativeTestResult;
    public int hasSelfDiagnosedPositiveBackgroundTick;
    public int completedQuestionnaireAndStartedIsolation;
    public int encounterDetectionPausedBackgroundTick;
    public int completedQuestionnaireButDidNotStartIsolation;
    public int totalBackgroundTasks;
    public int runningNormallyBackgroundTick;
    public int completedOnboarding;
    public Integer receivedVoidTestResultEnteredManually;
    public Integer receivedPositiveTestResultEnteredManually;
    public Integer receivedNegativeTestResultEnteredManually;
    public Integer receivedVoidTestResultViaPolling;
    public Integer receivedPositiveTestResultViaPolling;
    public Integer receivedNegativeTestResultViaPolling;
    public Integer hasSelfDiagnosedBackgroundTick;
    public Integer hasTestedPositiveBackgroundTick;
    public Integer isIsolatingForSelfDiagnosedBackgroundTick;
    public Integer isIsolatingForTestedPositiveBackgroundTick;
    public Integer isIsolatingForHadRiskyContactBackgroundTick;
    public Integer receivedRiskyContactNotification;
    public Integer startedIsolation;
    public Integer receivedPositiveTestResultWhenIsolatingDueToRiskyContact;
    public Integer receivedActiveIpcToken;
    public Integer haveActiveIpcTokenBackgroundTick;
    public Integer selectedIsolationPaymentsButton;
    public Integer launchedIsolationPaymentsApplication;
    public Integer receivedPositiveLFDTestResultViaPolling;
    public Integer receivedNegativeLFDTestResultViaPolling;
    public Integer receivedVoidLFDTestResultViaPolling;
    public Integer receivedPositiveLFDTestResultEnteredManually;
    public Integer receivedNegativeLFDTestResultEnteredManually;
    public Integer receivedVoidLFDTestResultEnteredManually;
    public Integer hasTestedLFDPositiveBackgroundTick;
    public Integer isIsolatingForTestedLFDPositiveBackgroundTick;
    public Integer totalExposureWindowsNotConsideredRisky;
    public Integer totalExposureWindowsConsideredRisky;
    public Integer acknowledgedStartOfIsolationDueToRiskyContact;
    public Integer hasRiskyContactNotificationsEnabledBackgroundTick;
    public Integer totalRiskyContactReminderNotifications;
    public Integer receivedUnconfirmedPositiveTestResult;
    public Integer isIsolatingForUnconfirmedTestBackgroundTick;
    public Integer launchedTestOrdering;
    public Integer didHaveSymptomsBeforeReceivedTestResult;
    public Integer didRememberOnsetSymptomsDateBeforeReceivedTestResult;
    public Integer didAskForSymptomsOnPositiveTestEntry;
    public Integer declaredNegativeResultFromDCT;
    public Integer receivedPositiveSelfRapidTestResultViaPolling;
    public Integer receivedNegativeSelfRapidTestResultViaPolling;
    public Integer receivedVoidSelfRapidTestResultViaPolling;
    public Integer receivedPositiveSelfRapidTestResultEnteredManually;
    public Integer receivedNegativeSelfRapidTestResultEnteredManually;
    public Integer receivedVoidSelfRapidTestResultEnteredManually;
    public Integer isIsolatingForTestedSelfRapidPositiveBackgroundTick;
    public Integer hasTestedSelfRapidPositiveBackgroundTick;
    public Integer receivedRiskyVenueM1Warning;
    public Integer receivedRiskyVenueM2Warning;
    public Integer hasReceivedRiskyVenueM2WarningBackgroundTick;

    public AnalyticsMetrics() {
    }
}
