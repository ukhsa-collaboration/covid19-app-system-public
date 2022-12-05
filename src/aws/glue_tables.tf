################################################################################
# THIS FILE IS AUTO-GENERATED, DO NOT EDIT DIRECTLY
# CHANGES TO TEMPLATE SHOULD BE MADE VIA: tools/templates/glue_tables_kinesis.tf.erb
# TO ADD NEW FIELDS, EDIT src/aws/analytics_fields/fields.json
################################################################################

# [PARQUET CONSOLIDATION] FIXME: columns need to be kept in sync with glue table below
resource "aws_glue_catalog_table" "mobile_analytics" {
  name          = "${terraform.workspace}_analytics"
  database_name = aws_glue_catalog_database.mobile_analytics.name

  table_type = "EXTERNAL_TABLE"

  parameters = {
    EXTERNAL              = "TRUE"
    "parquet.compression" = "SNAPPY"
  }

  storage_descriptor {
    location      = "s3://${module.analytics_submission_store_parquet.bucket_id}/"
    input_format  = "org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat"
    output_format = "org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat"

    ser_de_info {
      name                  = "my-stream"
      serialization_library = "org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe"

      parameters = {
        "serialization.format" = 1
      }
    }

    columns {
      name = "startDate"
      type = "string"
    }
    columns {
      name = "endDate"
      type = "string"
    }
    columns {
      name = "postalDistrict"
      type = "string"
    }
    columns {
      name = "localAuthority"
      type = "string"
    }
    columns {
      name = "deviceModel"
      type = "string"
    }
    columns {
      name = "latestApplicationVersion"
      type = "string"
    }
    columns {
      name = "operatingSystemVersion"
      type = "string"
    }
    columns {
      name = "cumulativeDownloadBytes"
      type = "int"
    }
    columns {
      name = "cumulativeUploadBytes"
      type = "int"
    }
    columns {
      name = "cumulativeCellularDownloadBytes"
      type = "int"
    }
    columns {
      name = "cumulativeCellularUploadBytes"
      type = "int"
    }
    columns {
      name = "cumulativeWifiDownloadBytes"
      type = "int"
    }
    columns {
      name = "cumulativeWifiUploadBytes"
      type = "int"
    }
    columns {
      name = "checkedIn"
      type = "int"
    }
    columns {
      name = "canceledCheckIn"
      type = "int"
    }
    columns {
      name = "receivedVoidTestResult"
      type = "int"
    }
    columns {
      name = "isIsolatingBackgroundTick"
      type = "int"
    }
    columns {
      name = "hasHadRiskyContactBackgroundTick"
      type = "int"
    }
    columns {
      name = "receivedPositiveTestResult"
      type = "int"
    }
    columns {
      name = "receivedNegativeTestResult"
      type = "int"
    }
    columns {
      name = "hasSelfDiagnosedPositiveBackgroundTick"
      type = "int"
    }
    columns {
      name = "completedQuestionnaireAndStartedIsolation"
      type = "int"
    }
    columns {
      name = "encounterDetectionPausedBackgroundTick"
      type = "int"
    }
    columns {
      name = "completedQuestionnaireButDidNotStartIsolation"
      type = "int"
    }
    columns {
      name = "totalBackgroundTasks"
      type = "int"
    }
    columns {
      name = "runningNormallyBackgroundTick"
      type = "int"
    }
    columns {
      name = "completedOnboarding"
      type = "int"
    }
    columns {
      name = "includesMultipleApplicationVersions"
      type = "boolean"
    }
    columns {
      name = "receivedVoidTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "receivedPositiveTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "receivedNegativeTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "receivedVoidTestResultViaPolling"
      type = "int"
    }
    columns {
      name = "receivedPositiveTestResultViaPolling"
      type = "int"
    }
    columns {
      name = "receivedNegativeTestResultViaPolling"
      type = "int"
    }
    columns {
      name = "hasSelfDiagnosedBackgroundTick"
      type = "int"
    }
    columns {
      name = "hasTestedPositiveBackgroundTick"
      type = "int"
    }
    columns {
      name = "isIsolatingForSelfDiagnosedBackgroundTick"
      type = "int"
    }
    columns {
      name = "isIsolatingForTestedPositiveBackgroundTick"
      type = "int"
    }
    columns {
      name = "isIsolatingForHadRiskyContactBackgroundTick"
      type = "int"
    }
    columns {
      name = "receivedRiskyContactNotification"
      type = "int"
    }
    columns {
      name = "startedIsolation"
      type = "int"
    }
    columns {
      name = "receivedActiveIpcToken"
      type = "int"
    }
    columns {
      name = "haveActiveIpcTokenBackgroundTick"
      type = "int"
    }
    columns {
      name = "selectedIsolationPaymentsButton"
      type = "int"
    }
    columns {
      name = "launchedIsolationPaymentsApplication"
      type = "int"
    }
    columns {
      name = "receivedPositiveLFDTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "hasTestedLFDPositiveBackgroundTick"
      type = "int"
    }
    columns {
      name = "isIsolatingForTestedLFDPositiveBackgroundTick"
      type = "int"
    }
    columns {
      name = "totalExposureWindowsNotConsideredRisky"
      type = "int"
    }
    columns {
      name = "totalExposureWindowsConsideredRisky"
      type = "int"
    }
    columns {
      name = "acknowledgedStartOfIsolationDueToRiskyContact"
      type = "int"
    }
    columns {
      name = "hasRiskyContactNotificationsEnabledBackgroundTick"
      type = "int"
    }
    columns {
      name = "totalRiskyContactReminderNotifications"
      type = "int"
    }
    columns {
      name = "receivedUnconfirmedPositiveTestResult"
      type = "int"
    }
    columns {
      name = "isIsolatingForUnconfirmedTestBackgroundTick"
      type = "int"
    }
    columns {
      name = "launchedTestOrdering"
      type = "int"
    }
    columns {
      name = "didHaveSymptomsBeforeReceivedTestResult"
      type = "int"
    }
    columns {
      name = "didRememberOnsetSymptomsDateBeforeReceivedTestResult"
      type = "int"
    }
    columns {
      name = "didAskForSymptomsOnPositiveTestEntry"
      type = "int"
    }
    columns {
      name = "receivedPositiveSelfRapidTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "isIsolatingForTestedSelfRapidPositiveBackgroundTick"
      type = "int"
    }
    columns {
      name = "hasTestedSelfRapidPositiveBackgroundTick"
      type = "int"
    }
    columns {
      name = "receivedRiskyVenueM1Warning"
      type = "int"
    }
    columns {
      name = "receivedRiskyVenueM2Warning"
      type = "int"
    }
    columns {
      name = "hasReceivedRiskyVenueM2WarningBackgroundTick"
      type = "int"
    }
    columns {
      name = "totalAlarmManagerBackgroundTasks"
      type = "int"
    }
    columns {
      name = "missingPacketsLast7Days"
      type = "int"
    }
    columns {
      name = "askedToShareExposureKeysInTheInitialFlow"
      type = "int"
    }
    columns {
      name = "consentedToShareExposureKeysInTheInitialFlow"
      type = "int"
    }
    columns {
      name = "totalShareExposureKeysReminderNotifications"
      type = "int"
    }
    columns {
      name = "consentedToShareExposureKeysInReminderScreen"
      type = "int"
    }
    columns {
      name = "successfullySharedExposureKeys"
      type = "int"
    }
    columns {
      name = "didSendLocalInfoNotification"
      type = "int"
    }
    columns {
      name = "didAccessLocalInfoScreenViaNotification"
      type = "int"
    }
    columns {
      name = "didAccessLocalInfoScreenViaBanner"
      type = "int"
    }
    columns {
      name = "isDisplayingLocalInfoBackgroundTick"
      type = "int"
    }
    columns {
      name = "positiveLabResultAfterPositiveLFD"
      type = "int"
    }
    columns {
      name = "negativeLabResultAfterPositiveLFDWithinTimeLimit"
      type = "int"
    }
    columns {
      name = "negativeLabResultAfterPositiveLFDOutsideTimeLimit"
      type = "int"
    }
    columns {
      name = "positiveLabResultAfterPositiveSelfRapidTest"
      type = "int"
    }
    columns {
      name = "negativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit"
      type = "int"
    }
    columns {
      name = "negativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit"
      type = "int"
    }
    columns {
      name = "didAccessRiskyVenueM2Notification"
      type = "int"
    }
    columns {
      name = "selectedTakeTestM2Journey"
      type = "int"
    }
    columns {
      name = "selectedTakeTestLaterM2Journey"
      type = "int"
    }
    columns {
      name = "selectedHasSymptomsM2Journey"
      type = "int"
    }
    columns {
      name = "selectedHasNoSymptomsM2Journey"
      type = "int"
    }
    columns {
      name = "selectedLFDTestOrderingM2Journey"
      type = "int"
    }
    columns {
      name = "selectedHasLFDTestM2Journey"
      type = "int"
    }
    columns {
      name = "optedOutForContactIsolation"
      type = "int"
    }
    columns {
      name = "optedOutForContactIsolationBackgroundTick"
      type = "int"
    }
    columns {
      name = "appIsUsableBackgroundTick"
      type = "int"
    }
    columns {
      name = "appIsContactTraceableBackgroundTick"
      type = "int"
    }
    columns {
      name = "didAccessSelfIsolationNoteLink"
      type = "int"
    }
    columns {
      name = "appIsUsableBluetoothOffBackgroundTick"
      type = "int"
    }
    columns {
      name = "completedV2SymptomsQuestionnaire"
      type = "int"
    }
    columns {
      name = "completedV2SymptomsQuestionnaireAndStayAtHome"
      type = "int"
    }
    columns {
      name = "hasCompletedV2SymptomsQuestionnaireBackgroundTick"
      type = "int"
    }
    columns {
      name = "hasCompletedV2SymptomsQuestionnaireAndStayAtHomeBackgroundTick"
      type = "int"
    }
    columns {
      name = "completedV3SymptomsQuestionnaireAndHasSymptoms"
      type = "int"
    }
    columns {
      name = "selfReportedVoidSelfLFDTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "selfReportedNegativeSelfLFDTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "isPositiveSelfLFDFree"
      type = "int"
    }
    columns {
      name = "selfReportedPositiveSelfLFDOnGov"
      type = "int"
    }
    columns {
      name = "completedSelfReportingTestFlow"
      type = "int"
    }
  }
}

# [PARQUET CONSOLIDATION] FIXME: columns need to be kept in sync with glue table above
resource "aws_glue_catalog_table" "mobile_analytics_consolidated" {
  name          = "${terraform.workspace}_analytics_consolidated"
  database_name = aws_glue_catalog_database.mobile_analytics.name

  table_type = "EXTERNAL_TABLE"

  parameters = {
    EXTERNAL              = "TRUE"
    "parquet.compression" = "SNAPPY"
  }

  storage_descriptor {
    location      = "s3://${module.analytics_submission_store_parquet_consolidated.bucket_id}/"
    input_format  = "org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat"
    output_format = "org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat"

    ser_de_info {
      name                  = "my-stream"
      serialization_library = "org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe"

      parameters = {
        "serialization.format" = 1
      }
    }

    columns {
      name = "startDate"
      type = "string"
    }
    columns {
      name = "endDate"
      type = "string"
    }
    columns {
      name = "postalDistrict"
      type = "string"
    }
    columns {
      name = "localAuthority"
      type = "string"
    }
    columns {
      name = "deviceModel"
      type = "string"
    }
    columns {
      name = "latestApplicationVersion"
      type = "string"
    }
    columns {
      name = "operatingSystemVersion"
      type = "string"
    }
    columns {
      name = "cumulativeDownloadBytes"
      type = "int"
    }
    columns {
      name = "cumulativeUploadBytes"
      type = "int"
    }
    columns {
      name = "cumulativeCellularDownloadBytes"
      type = "int"
    }
    columns {
      name = "cumulativeCellularUploadBytes"
      type = "int"
    }
    columns {
      name = "cumulativeWifiDownloadBytes"
      type = "int"
    }
    columns {
      name = "cumulativeWifiUploadBytes"
      type = "int"
    }
    columns {
      name = "checkedIn"
      type = "int"
    }
    columns {
      name = "canceledCheckIn"
      type = "int"
    }
    columns {
      name = "receivedVoidTestResult"
      type = "int"
    }
    columns {
      name = "isIsolatingBackgroundTick"
      type = "int"
    }
    columns {
      name = "hasHadRiskyContactBackgroundTick"
      type = "int"
    }
    columns {
      name = "receivedPositiveTestResult"
      type = "int"
    }
    columns {
      name = "receivedNegativeTestResult"
      type = "int"
    }
    columns {
      name = "hasSelfDiagnosedPositiveBackgroundTick"
      type = "int"
    }
    columns {
      name = "completedQuestionnaireAndStartedIsolation"
      type = "int"
    }
    columns {
      name = "encounterDetectionPausedBackgroundTick"
      type = "int"
    }
    columns {
      name = "completedQuestionnaireButDidNotStartIsolation"
      type = "int"
    }
    columns {
      name = "totalBackgroundTasks"
      type = "int"
    }
    columns {
      name = "runningNormallyBackgroundTick"
      type = "int"
    }
    columns {
      name = "completedOnboarding"
      type = "int"
    }
    columns {
      name = "includesMultipleApplicationVersions"
      type = "boolean"
    }
    columns {
      name = "receivedVoidTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "receivedPositiveTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "receivedNegativeTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "receivedVoidTestResultViaPolling"
      type = "int"
    }
    columns {
      name = "receivedPositiveTestResultViaPolling"
      type = "int"
    }
    columns {
      name = "receivedNegativeTestResultViaPolling"
      type = "int"
    }
    columns {
      name = "hasSelfDiagnosedBackgroundTick"
      type = "int"
    }
    columns {
      name = "hasTestedPositiveBackgroundTick"
      type = "int"
    }
    columns {
      name = "isIsolatingForSelfDiagnosedBackgroundTick"
      type = "int"
    }
    columns {
      name = "isIsolatingForTestedPositiveBackgroundTick"
      type = "int"
    }
    columns {
      name = "isIsolatingForHadRiskyContactBackgroundTick"
      type = "int"
    }
    columns {
      name = "receivedRiskyContactNotification"
      type = "int"
    }
    columns {
      name = "startedIsolation"
      type = "int"
    }
    columns {
      name = "receivedActiveIpcToken"
      type = "int"
    }
    columns {
      name = "haveActiveIpcTokenBackgroundTick"
      type = "int"
    }
    columns {
      name = "selectedIsolationPaymentsButton"
      type = "int"
    }
    columns {
      name = "launchedIsolationPaymentsApplication"
      type = "int"
    }
    columns {
      name = "receivedPositiveLFDTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "hasTestedLFDPositiveBackgroundTick"
      type = "int"
    }
    columns {
      name = "isIsolatingForTestedLFDPositiveBackgroundTick"
      type = "int"
    }
    columns {
      name = "totalExposureWindowsNotConsideredRisky"
      type = "int"
    }
    columns {
      name = "totalExposureWindowsConsideredRisky"
      type = "int"
    }
    columns {
      name = "acknowledgedStartOfIsolationDueToRiskyContact"
      type = "int"
    }
    columns {
      name = "hasRiskyContactNotificationsEnabledBackgroundTick"
      type = "int"
    }
    columns {
      name = "totalRiskyContactReminderNotifications"
      type = "int"
    }
    columns {
      name = "receivedUnconfirmedPositiveTestResult"
      type = "int"
    }
    columns {
      name = "isIsolatingForUnconfirmedTestBackgroundTick"
      type = "int"
    }
    columns {
      name = "launchedTestOrdering"
      type = "int"
    }
    columns {
      name = "didHaveSymptomsBeforeReceivedTestResult"
      type = "int"
    }
    columns {
      name = "didRememberOnsetSymptomsDateBeforeReceivedTestResult"
      type = "int"
    }
    columns {
      name = "didAskForSymptomsOnPositiveTestEntry"
      type = "int"
    }
    columns {
      name = "receivedPositiveSelfRapidTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "isIsolatingForTestedSelfRapidPositiveBackgroundTick"
      type = "int"
    }
    columns {
      name = "hasTestedSelfRapidPositiveBackgroundTick"
      type = "int"
    }
    columns {
      name = "receivedRiskyVenueM1Warning"
      type = "int"
    }
    columns {
      name = "receivedRiskyVenueM2Warning"
      type = "int"
    }
    columns {
      name = "hasReceivedRiskyVenueM2WarningBackgroundTick"
      type = "int"
    }
    columns {
      name = "totalAlarmManagerBackgroundTasks"
      type = "int"
    }
    columns {
      name = "missingPacketsLast7Days"
      type = "int"
    }
    columns {
      name = "askedToShareExposureKeysInTheInitialFlow"
      type = "int"
    }
    columns {
      name = "consentedToShareExposureKeysInTheInitialFlow"
      type = "int"
    }
    columns {
      name = "totalShareExposureKeysReminderNotifications"
      type = "int"
    }
    columns {
      name = "consentedToShareExposureKeysInReminderScreen"
      type = "int"
    }
    columns {
      name = "successfullySharedExposureKeys"
      type = "int"
    }
    columns {
      name = "didSendLocalInfoNotification"
      type = "int"
    }
    columns {
      name = "didAccessLocalInfoScreenViaNotification"
      type = "int"
    }
    columns {
      name = "didAccessLocalInfoScreenViaBanner"
      type = "int"
    }
    columns {
      name = "isDisplayingLocalInfoBackgroundTick"
      type = "int"
    }
    columns {
      name = "positiveLabResultAfterPositiveLFD"
      type = "int"
    }
    columns {
      name = "negativeLabResultAfterPositiveLFDWithinTimeLimit"
      type = "int"
    }
    columns {
      name = "negativeLabResultAfterPositiveLFDOutsideTimeLimit"
      type = "int"
    }
    columns {
      name = "positiveLabResultAfterPositiveSelfRapidTest"
      type = "int"
    }
    columns {
      name = "negativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit"
      type = "int"
    }
    columns {
      name = "negativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit"
      type = "int"
    }
    columns {
      name = "didAccessRiskyVenueM2Notification"
      type = "int"
    }
    columns {
      name = "selectedTakeTestM2Journey"
      type = "int"
    }
    columns {
      name = "selectedTakeTestLaterM2Journey"
      type = "int"
    }
    columns {
      name = "selectedHasSymptomsM2Journey"
      type = "int"
    }
    columns {
      name = "selectedHasNoSymptomsM2Journey"
      type = "int"
    }
    columns {
      name = "selectedLFDTestOrderingM2Journey"
      type = "int"
    }
    columns {
      name = "selectedHasLFDTestM2Journey"
      type = "int"
    }
    columns {
      name = "optedOutForContactIsolation"
      type = "int"
    }
    columns {
      name = "optedOutForContactIsolationBackgroundTick"
      type = "int"
    }
    columns {
      name = "appIsUsableBackgroundTick"
      type = "int"
    }
    columns {
      name = "appIsContactTraceableBackgroundTick"
      type = "int"
    }
    columns {
      name = "didAccessSelfIsolationNoteLink"
      type = "int"
    }
    columns {
      name = "appIsUsableBluetoothOffBackgroundTick"
      type = "int"
    }
    columns {
      name = "completedV2SymptomsQuestionnaire"
      type = "int"
    }
    columns {
      name = "completedV2SymptomsQuestionnaireAndStayAtHome"
      type = "int"
    }
    columns {
      name = "hasCompletedV2SymptomsQuestionnaireBackgroundTick"
      type = "int"
    }
    columns {
      name = "hasCompletedV2SymptomsQuestionnaireAndStayAtHomeBackgroundTick"
      type = "int"
    }
    columns {
      name = "completedV3SymptomsQuestionnaireAndHasSymptoms"
      type = "int"
    }
    columns {
      name = "selfReportedVoidSelfLFDTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "selfReportedNegativeSelfLFDTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "isPositiveSelfLFDFree"
      type = "int"
    }
    columns {
      name = "selfReportedPositiveSelfLFDOnGov"
      type = "int"
    }
    columns {
      name = "completedSelfReportingTestFlow"
      type = "int"
    }
  }
}

resource "aws_glue_catalog_table" "mobile_events_analytics" {
  name          = "${terraform.workspace}_analytics_events"
  database_name = aws_glue_catalog_database.mobile_analytics.name

  table_type = "EXTERNAL_TABLE"

  parameters = {
    EXTERNAL              = "TRUE"
    "parquet.compression" = "SNAPPY"
  }

  storage_descriptor {
    location      = "s3://${module.analytics_events_submission_store_parquet.bucket_id}/"
    input_format  = "org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat"
    output_format = "org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat"

    ser_de_info {
      name                  = "my-stream"
      serialization_library = "org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe"

      parameters = {
        "serialization.format" = 1
      }
    }

    columns {
      name = "type"
      type = "string"
    }
    columns {
      name = "version"
      type = "int"
    }
    columns {
      name = "riskScore"
      type = "double"
    }
    columns {
      name = "riskCalculationVersion"
      type = "int"
    }
    columns {
      name = "infectiousness"
      type = "string"
    }
    columns {
      name = "isConsideredRisky"
      type = "boolean"
    }
    columns {
      name = "scanInstances"
      type = "array<struct<secondsSinceLastScan:int,minimumAttenuation:int,typicalAttenuation:int>>"
    }
    columns {
      name = "date"
      type = "string"
    }
    columns {
      name = "operatingSystemVersion"
      type = "string"
    }
    columns {
      name = "localAuthority"
      type = "string"
    }
    columns {
      name = "latestApplicationVersion"
      type = "string"
    }
    columns {
      name = "deviceModel"
      type = "string"
    }
    columns {
      name = "postalDistrict"
      type = "string"
    }
    columns {
      name = "uuid"
      type = "string"
    }
  }
}
