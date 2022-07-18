package uk.nhs.nhsx.analyticssubmission.model;

public class AnalyticsMetrics {

    public Long cumulativeDownloadBytes;
    public Long cumulativeUploadBytes;
    public Long cumulativeCellularDownloadBytes;
    public Long cumulativeCellularUploadBytes;
    public Long cumulativeWifiDownloadBytes;
    public Long cumulativeWifiUploadBytes;
    public int receivedVoidTestResult;
    public int isIsolatingBackgroundTick;
    public int hasHadRiskyContactBackgroundTick;
    public int receivedPositiveTestResult;
    public int receivedNegativeTestResult;
    public int encounterDetectionPausedBackgroundTick;
    public int totalBackgroundTasks;
    public int runningNormallyBackgroundTick;
    public int completedOnboarding;
    public Integer receivedVoidTestResultEnteredManually;
    public Integer receivedPositiveTestResultEnteredManually;
    public Integer receivedNegativeTestResultEnteredManually;
    public Integer receivedVoidTestResultViaPolling;
    public Integer receivedPositiveTestResultViaPolling;
    public Integer receivedNegativeTestResultViaPolling;
    public Integer isIsolatingForTestedPositiveBackgroundTick;
    public Integer receivedRiskyContactNotification;
    public Integer startedIsolation;
    public Integer receivedActiveIpcToken;
    public Integer haveActiveIpcTokenBackgroundTick;
    public Integer selectedIsolationPaymentsButton;
    public Integer launchedIsolationPaymentsApplication;
    public Integer receivedPositiveLFDTestResultEnteredManually;
    public Integer isIsolatingForTestedLFDPositiveBackgroundTick;
    public Integer totalExposureWindowsNotConsideredRisky;
    public Integer totalExposureWindowsConsideredRisky;
    public Integer hasRiskyContactNotificationsEnabledBackgroundTick;
    public Integer totalRiskyContactReminderNotifications;
    public Integer receivedUnconfirmedPositiveTestResult;
    public Integer isIsolatingForUnconfirmedTestBackgroundTick;
    public Integer launchedTestOrdering;
    public Integer didHaveSymptomsBeforeReceivedTestResult;
    public Integer didRememberOnsetSymptomsDateBeforeReceivedTestResult;
    public Integer receivedPositiveSelfRapidTestResultEnteredManually;
    public Integer isIsolatingForTestedSelfRapidPositiveBackgroundTick;
    public Integer totalAlarmManagerBackgroundTasks;
    public Integer missingPacketsLast7Days;
    public Integer askedToShareExposureKeysInTheInitialFlow;
    public Integer consentedToShareExposureKeysInTheInitialFlow;
    public Integer totalShareExposureKeysReminderNotifications;
    public Integer consentedToShareExposureKeysInReminderScreen;
    public Integer successfullySharedExposureKeys;
    public Integer didSendLocalInfoNotification;
    public Integer didAccessLocalInfoScreenViaNotification;
    public Integer didAccessLocalInfoScreenViaBanner;
    public Integer isDisplayingLocalInfoBackgroundTick;
    public Integer positiveLabResultAfterPositiveLFD;
    public Integer negativeLabResultAfterPositiveLFDWithinTimeLimit;
    public Integer negativeLabResultAfterPositiveLFDOutsideTimeLimit;
    public Integer positiveLabResultAfterPositiveSelfRapidTest;
    public Integer negativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit;
    public Integer negativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit;
    public Integer optedOutForContactIsolation;
    public Integer optedOutForContactIsolationBackgroundTick;
    public Integer appIsUsableBackgroundTick;
    public Integer appIsContactTraceableBackgroundTick;
    public Integer appIsUsableBluetoothOffBackgroundTick;
    public Integer completedV2SymptomsQuestionnaire;
    public Integer completedV2SymptomsQuestionnaireAndStayAtHome;


    public AnalyticsMetrics() {
    }
}
