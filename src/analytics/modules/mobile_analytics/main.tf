locals {
  table_name = "${terraform.workspace}_analytics_mobile"
}

resource "aws_glue_catalog_table" "this" {
  name          = local.table_name
  database_name = var.database_name

  table_type = "EXTERNAL_TABLE"

  parameters = {
    EXTERNAL                                     = "TRUE"
    "parquet.compression"                        = "SNAPPY"
    "projection.submitteddatehour.type"          = "date"
    "projection.submitteddatehour.range"         = "2020/01/01/00,NOW"
    "projection.submitteddatehour.format"        = "yyyy/MM/dd/HH"
    "projection.submitteddatehour.interval"      = 1
    "projection.submitteddatehour.interval.unit" = "HOURS"
    "projection.enabled"                         = true
    "storage.location.template"                  = "s3://${var.analytics_submission_store_parquet_bucket_id}/$${submitteddatehour}"
  }

  partition_keys {
    name = "submitteddatehour"
    type = "string"
  }

  storage_descriptor {
    location      = "s3://${var.analytics_submission_store_parquet_bucket_id}/"
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
      name = "receivedPositiveTestResultWhenIsolatingDueToRiskyContact"
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
      name = "receivedPositiveLFDTestResultViaPolling"
      type = "int"
    }
    columns {
      name = "receivedNegativeLFDTestResultViaPolling"
      type = "int"
    }
    columns {
      name = "receivedVoidLFDTestResultViaPolling"
      type = "int"
    }
    columns {
      name = "receivedPositiveLFDTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "receivedNegativeLFDTestResultEnteredManually"
      type = "int"
    }
    columns {
      name = "receivedVoidLFDTestResultEnteredManually"
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

  }
}


resource "aws_athena_named_query" "this" {
  name      = local.table_name
  database  = var.database_name
  workgroup = var.workgroup_name
  query     = <<EOF
    SELECT * FROM "${local.table_name}"
    SELECT
      "substring"("startdate", 1, 10) "truncatedstartdate"
    , "substring"("enddate", 1, 10) "truncatedenddate"
    , *
    FROM "${local.table_name}"
  EOF
}
