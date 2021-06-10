package smoke.data

import uk.nhs.nhsx.analyticssubmission.model.AnalyticsMetrics

object AnalyticsMetricsData {
    fun populatedAnalyticsMetrics() = AnalyticsMetrics().apply {
        var counter = 0L
        cumulativeDownloadBytes = counter++
        cumulativeUploadBytes = counter++
        cumulativeCellularDownloadBytes = counter++
        cumulativeCellularUploadBytes = counter++
        cumulativeWifiDownloadBytes = counter++
        cumulativeWifiUploadBytes = counter++
        checkedIn = counter++.toInt()
        canceledCheckIn = counter++.toInt()
        receivedVoidTestResult = counter++.toInt()
        isIsolatingBackgroundTick = counter++.toInt()
        hasHadRiskyContactBackgroundTick = counter++.toInt()
        receivedPositiveTestResult = counter++.toInt()
        receivedNegativeTestResult = counter++.toInt()
        hasSelfDiagnosedPositiveBackgroundTick = counter++.toInt()
        completedQuestionnaireAndStartedIsolation = counter++.toInt()
        encounterDetectionPausedBackgroundTick = counter++.toInt()
        completedQuestionnaireButDidNotStartIsolation = counter++.toInt()
        totalBackgroundTasks = counter++.toInt()
        runningNormallyBackgroundTick = counter++.toInt()
        completedOnboarding = counter++.toInt()
        receivedVoidTestResultEnteredManually = counter++.toInt()
        receivedPositiveTestResultEnteredManually = counter++.toInt()
        receivedNegativeTestResultEnteredManually = counter++.toInt()
        receivedVoidTestResultViaPolling = counter++.toInt()
        receivedPositiveTestResultViaPolling = counter++.toInt()
        receivedNegativeTestResultViaPolling = counter++.toInt()
        hasSelfDiagnosedBackgroundTick = counter++.toInt()
        hasTestedPositiveBackgroundTick = counter++.toInt()
        isIsolatingForSelfDiagnosedBackgroundTick = counter++.toInt()
        isIsolatingForTestedPositiveBackgroundTick = counter++.toInt()
        isIsolatingForHadRiskyContactBackgroundTick = counter++.toInt()
        receivedRiskyContactNotification = counter++.toInt()
        startedIsolation = counter++.toInt()
        receivedPositiveTestResultWhenIsolatingDueToRiskyContact = counter++.toInt()
        receivedActiveIpcToken = counter++.toInt()
        haveActiveIpcTokenBackgroundTick = counter++.toInt()
        selectedIsolationPaymentsButton = counter++.toInt()
        launchedIsolationPaymentsApplication = counter++.toInt()
        receivedPositiveLFDTestResultViaPolling = counter++.toInt()
        receivedNegativeLFDTestResultViaPolling = counter++.toInt()
        receivedVoidLFDTestResultViaPolling = counter++.toInt()
        receivedPositiveLFDTestResultEnteredManually = counter++.toInt()
        receivedNegativeLFDTestResultEnteredManually = counter++.toInt()
        receivedVoidLFDTestResultEnteredManually = counter++.toInt()
        hasTestedLFDPositiveBackgroundTick = counter++.toInt()
        isIsolatingForTestedLFDPositiveBackgroundTick = counter++.toInt()
        totalExposureWindowsNotConsideredRisky = counter++.toInt()
        totalExposureWindowsConsideredRisky = counter.toInt()
    }
}
