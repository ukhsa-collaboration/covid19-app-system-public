locals {
  query_name_prefix = "${var.database_name}_db"
}

resource "aws_athena_workgroup" "this" {
  name          = "${terraform.workspace}_workgroup"
  force_destroy = true

  configuration {
    enforce_workgroup_configuration = true

    result_configuration {
      # we do not set an output_location here because we want to be able to rename the folders our results land in.
      # this is done in AthenaService.java

      encryption_configuration {
        encryption_option = "SSE_S3"
      }
    }
  }
}

# Metric: onboardingCompletedToday
# completedOnboarding is an int, meaning that the same person can onboard multiple time (a day)
# note that this is unexpected behaviour and should not be an issue (though is worth noting)
resource "aws_athena_named_query" "onboardingCompletedToday" {
  name      = "${local.query_name_prefix}_onboardingCompletedToday"
  database  = var.database_name
  workgroup = aws_athena_workgroup.this.name
  query     = <<EOF
    SELECT
      SUBSTRING(endDate, 1, 10) AS date,
      CAST(SUM(completedOnboarding) AS DECIMAL) AS onboarding_completed_today
    FROM "${var.table_name}"
    WHERE TO_DATE(SUBSTRING(endDate,1,10),'yyyy-mm-dd') <= current_date
    GROUP BY SUBSTRING(endDate, 1, 10)
    ORDER BY date DESC
  EOF
}

# Metric: activeUsers
resource "aws_athena_named_query" "activeUsers" {
  name      = "${local.query_name_prefix}_activeUsers"
  database  = var.database_name
  workgroup = aws_athena_workgroup.this.name
  query     = <<EOF
  SELECT SUBSTRING(endDate, 1, 10) AS date,
    CAST(COUNT(CASE WHEN deviceModel LIKE 'iPhone%' THEN 1 ELSE NULL END) AS DECIMAL) AS active_users_ios,
    CAST(COUNT(CASE WHEN deviceModel NOT LIKE 'iPhone%' THEN 1 ELSE NULL END) AS DECIMAL) AS active_users_android,
    CAST(COUNT(totalBackgroundTasks) AS DECIMAL) AS active_users_total
  FROM "${var.table_name}"
  WHERE totalBackgroundTasks > 0 AND TO_DATE(SUBSTRING(endDate,1,10),'yyyy-mm-dd') <= current_date
  GROUP BY SUBSTRING(endDate, 1, 10)
  ORDER BY date DESC
  EOF
}

# Metric: dataDownloadUsageBytes
resource "aws_athena_named_query" "dataUsageBytes" {
  name      = "${local.query_name_prefix}_dataUsageBytes"
  database  = var.database_name
  workgroup = aws_athena_workgroup.this.name
  query     = <<EOF
    SELECT SUBSTRING(endDate, 1, 10) AS date,
      CAST(SUM(cumulativeDownloadBytes) AS DECIMAL) AS total_data_download_usage,
      CAST(SUM(cumulativeUploadBytes) AS DECIMAL) AS total_data_upload_usage
    FROM "${var.table_name}"
    WHERE TO_DATE(SUBSTRING(endDate,1,10),'yyyy-mm-dd') <= current_date
    GROUP BY SUBSTRING(endDate, 1, 10)
    ORDER BY date DESC
  EOF
}


# Metric: dataDownloadUsageBytesAverage
resource "aws_athena_named_query" "dataUsageBytesAverage" {
  name      = "${local.query_name_prefix}_dataUsageBytesAverage"
  database  = var.database_name
  workgroup = aws_athena_workgroup.this.name
  query     = <<EOF
    SELECT SUBSTRING(endDate, 1, 10) AS date,
      CAST(AVG(cumulativeDownloadBytes) AS DECIMAL) AS average_download_usage,
      CAST(AVG(cumulativeUploadBytes) AS DECIMAL) AS average_upload_usage
    FROM "${var.table_name}"
    WHERE TO_DATE(SUBSTRING(endDate,1,10),'yyyy-mm-dd') <= current_date
    GROUP BY SUBSTRING(endDate, 1, 10)
    ORDER BY date DESC
  EOF
}

# Metric: qrCodeCheckInCounts
resource "aws_athena_named_query" "qrCodeCheckInCounts" {
  name      = "${local.query_name_prefix}_qrCodeCheckInCounts"
  database  = var.database_name
  workgroup = aws_athena_workgroup.this.name
  query     = <<EOF
    SELECT SUBSTRING(endDate, 1, 10) AS date,
      CAST(SUM(checkedIn) AS DECIMAL) AS total_qr_checkins,
      CAST(SUM(canceledCheckIn) AS DECIMAL) AS total_canceled_qr_checkins
    FROM "${var.table_name}"
    WHERE TO_DATE(SUBSTRING(endDate,1,10),'yyyy-mm-dd') <= current_date
    GROUP BY SUBSTRING(endDate, 1, 10)
    ORDER BY date DESC
  EOF
}

# Metric: isolationStatus
# currently assumes that isIsolatingBackgroundTick > 0 counts as a day isolated
resource "aws_athena_named_query" "isolationStatus" {
  name      = "${local.query_name_prefix}_isolationStatus"
  database  = var.database_name
  workgroup = aws_athena_workgroup.this.name
  query     = <<EOF
    SELECT SUBSTRING(endDate, 1, 10) AS date,
      CAST(COUNT(isIsolatingBackgroundTick) AS DECIMAL) AS total_isolations
    FROM "${var.table_name}"
    WHERE isIsolatingBackgroundTick>0 AND TO_DATE(SUBSTRING(endDate,1,10),'yyyy-mm-dd') <= current_date
    GROUP BY SUBSTRING(endDate, 1, 10)
    ORDER BY date DESC
  EOF
}

# Metric: symptomaticQuestionnaireResults
resource "aws_athena_named_query" "symptomaticQuestionnaireResults" {
  name      = "${local.query_name_prefix}_symptomaticQuestionnaireResults"
  database  = var.database_name
  workgroup = aws_athena_workgroup.this.name
  query     = <<EOF
    SELECT
      SUBSTRING(endDate, 1, 10) AS date,
      CAST(COUNT(CASE WHEN completedQuestionnaireAndStartedIsolation>0 THEN 1 END) AS DECIMAL) AS positive,
      CAST(COUNT(CASE WHEN completedQuestionnaireButDidNotStartIsolation>0 THEN 1 END) AS DECIMAL) AS negative
    FROM "${var.table_name}"
    WHERE TO_DATE(SUBSTRING(endDate,1,10),'yyyy-mm-dd') <= current_date
    GROUP BY SUBSTRING(endDate, 1, 10)
    ORDER BY date DESC
  EOF
}

# Metric: virologyExamination
resource "aws_athena_named_query" "virologyExamination" {
  name      = "${local.query_name_prefix}_virologyExamination"
  database  = var.database_name
  workgroup = aws_athena_workgroup.this.name
  query     = <<EOF
    SELECT SUBSTRING(endDate, 1, 10) AS date,
      CAST(COUNT(receivedpositivetestresult) AS DECIMAL) AS void_test_result,
      CAST(COUNT(receivedpositivetestresult) AS DECIMAL) AS positive_test_result,
      CAST(COUNT(receivednegativetestresult) AS DECIMAL) AS negative_test_result,
      CAST(SUM(IF(completedQuestionnaireAndStartedIsolation>0,1,0)) AS DECIMAL) AS recommended_to_take_test
    FROM "${var.table_name}"
    WHERE completedQuestionnaireAndStartedIsolation>0 AND TO_DATE(SUBSTRING(endDate,1,10),'yyyy-mm-dd') <= current_date
    GROUP BY SUBSTRING(endDate, 1, 10)
    ORDER BY date DESC
  EOF
}

# Metric: pauseUsage
resource "aws_athena_named_query" "pauseUsage" {
  name      = "${local.query_name_prefix}_pauseUsage"
  database  = var.database_name
  workgroup = aws_athena_workgroup.this.name
  query     = <<EOF
    SELECT SUBSTRING(endDate, 1, 10) AS date,
      CAST(COUNT(encounterDetectionPausedBackgroundTick) AS DECIMAL) AS paused_background_tick
    FROM "${var.table_name}"
    WHERE TO_DATE(SUBSTRING(endDate,1,10),'yyyy-mm-dd') <= current_date
    GROUP BY SUBSTRING(endDate, 1, 10)
    ORDER BY date DESC
  EOF
}
