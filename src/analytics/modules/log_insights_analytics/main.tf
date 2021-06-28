resource "aws_glue_catalog_table" "exposure_notification_circuit_breaker_analytics" {
  name          = "${terraform.workspace}_analytics_exposure_notification_circuit_breaker"
  database_name = var.database_name
  table_type    = "EXTERNAL_TABLE"
  parameters = {
    "projection.startdate.type"          = "date"
    "projection.startdate.range"         = "2020/01/01,NOW"
    "projection.startdate.format"        = "yyyy/MM/dd"
    "projection.startdate.interval"      = 1
    "projection.startdate.interval.unit" = "DAYS"
    "projection.enabled"                 = true
    "storage.location.template"          = "s3://${var.circuit_breaker_stats_bucket_id}/$${startdate}"

  }
  partition_keys {
    name = "startDate"
    type = "string"
  }

  storage_descriptor {
    location      = "s3://${var.circuit_breaker_stats_bucket_id}/"
    input_format  = "org.apache.hadoop.mapred.TextInputFormat"
    output_format = "org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat"

    ser_de_info {
      name                  = "json"
      serialization_library = "org.openx.data.jsonserde.JsonSerDe"
    }


    columns {
      name = "startOfHour"
      type = "string"
    }
    columns {
      name = "exposureNotificationCBCount"
      type = "int"
    }
    columns {
      name = "iOSExposureNotificationCBCount"
      type = "int"
    }
    columns {
      name = "androidExposureNotificationCBCount"
      type = "int"
    }
    columns {
      name = "uniqueRequestIds"
      type = "int"
    }
    columns {
      name = "appVersion"
      type = "string"
    }
  }


}

resource "aws_glue_catalog_table" "key_federation_upload_analytics" {
  name          = "${terraform.workspace}_analytics_key_federation_upload"
  database_name = var.database_name
  table_type    = "EXTERNAL_TABLE"
  parameters = {
    "projection.startdate.type"          = "date"
    "projection.startdate.range"         = "2020/01/01,NOW"
    "projection.startdate.format"        = "yyyy/MM/dd"
    "projection.startdate.interval"      = 1
    "projection.startdate.interval.unit" = "DAYS"
    "projection.enabled"                 = true
    "storage.location.template"          = "s3://${var.key_federation_upload_stats_bucket_id}/$${startdate}"

  }
  partition_keys {
    name = "startDate"
    type = "string"
  }

  storage_descriptor {
    location      = "s3://${var.key_federation_upload_stats_bucket_id}/"
    input_format  = "org.apache.hadoop.mapred.TextInputFormat"
    output_format = "org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat"

    ser_de_info {
      name                  = "json"
      serialization_library = "org.openx.data.jsonserde.JsonSerDe"
    }


    columns {
      name = "startOfHour"
      type = "string"
    }
    columns {
      name = "testType"
      type = "int"
    }
    columns {
      name = "numberOfKeysUploaded"
      type = "int"
    }
  }
}
resource "aws_glue_catalog_table" "key_federation_download_analytics" {
  name          = "${terraform.workspace}_analytics_key_federation_download"
  database_name = var.database_name
  table_type    = "EXTERNAL_TABLE"
  parameters = {
    "projection.startdate.type"          = "date"
    "projection.startdate.range"         = "2020/01/01,NOW"
    "projection.startdate.format"        = "yyyy/MM/dd"
    "projection.startdate.interval"      = 1
    "projection.startdate.interval.unit" = "DAYS"
    "projection.enabled"                 = true
    "storage.location.template"          = "s3://${var.key_federation_download_stats_bucket_id}/$${startdate}"

  }
  partition_keys {
    name = "startDate"
    type = "string"
  }

  storage_descriptor {
    location      = "s3://${var.key_federation_download_stats_bucket_id}/"
    input_format  = "org.apache.hadoop.mapred.TextInputFormat"
    output_format = "org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat"

    ser_de_info {
      name                  = "json"
      serialization_library = "org.openx.data.jsonserde.JsonSerDe"
    }


    columns {
      name = "startOfHour"
      type = "string"
    }
    columns {
      name = "origin"
      type = "string"
    }
    columns {
      name = "testType"
      type = "int"
    }
    columns {
      name = "numberOfKeysDownloaded"
      type = "int"
    }
    columns {
      name = "numberOfKeysImported"
      type = "int"
    }
  }
}

resource "aws_glue_catalog_table" "diagnosis_key_submission_stats_analytics" {
  name          = "${terraform.workspace}_analytics_diagnosis_key_submission_stats"
  database_name = var.database_name
  table_type    = "EXTERNAL_TABLE"
  parameters = {
    "projection.startdate.type"          = "date"
    "projection.startdate.range"         = "2020/01/01,NOW"
    "projection.startdate.format"        = "yyyy/MM/dd"
    "projection.startdate.interval"      = 1
    "projection.startdate.interval.unit" = "DAYS"
    "projection.enabled"                 = true
    "storage.location.template"          = "s3://${var.diagnosis_key_submission_stats_bucket_id}/$${startdate}"

  }
  partition_keys {
    name = "startDate"
    type = "string"
  }

  storage_descriptor {
    location      = "s3://${var.diagnosis_key_submission_stats_bucket_id}/"
    input_format  = "org.apache.hadoop.mapred.TextInputFormat"
    output_format = "org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat"

    ser_de_info {
      name                  = "json"
      serialization_library = "org.openx.data.jsonserde.JsonSerDe"
    }


    columns {
      name = "startOfHour"
      type = "string"
    }
    columns {
      name = "platform"
      type = "string"
    }
    columns {
      name = "version"
      type = "string"
    }
    columns {
      name = "diagnosisKeysCount"
      type = "int"
    }
  }
}
resource "aws_glue_catalog_table" "cta_token_gen_stats_analytics" {
  name          = "${terraform.workspace}_analytics_cta_token_gen_stats"
  database_name = var.database_name
  table_type    = "EXTERNAL_TABLE"
  parameters = {
    "projection.startdate.type"          = "date"
    "projection.startdate.range"         = "2020/01/01,NOW"
    "projection.startdate.format"        = "yyyy/MM/dd"
    "projection.startdate.interval"      = 1
    "projection.startdate.interval.unit" = "DAYS"
    "projection.enabled"                 = true
    "storage.location.template"          = "s3://${var.virology_test_stats_bucket_id}/cta-token-gen/$${startdate}"

  }
  partition_keys {
    name = "startDate"
    type = "string"
  }

  storage_descriptor {
    location      = "s3://${var.virology_test_stats_bucket_id}/cta-token-gen/"
    input_format  = "org.apache.hadoop.mapred.TextInputFormat"
    output_format = "org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat"

    ser_de_info {
      name                  = "json"
      serialization_library = "org.openx.data.jsonserde.JsonSerDe"
    }


    columns {
      name = "testType"
      type = "string"
    }
    columns {
      name = "source"
      type = "string"
    }
    columns {
      name = "resultType"
      type = "string"
    }
    columns {
      name = "total"
      type = "int"
    }
  }
}

resource "aws_glue_catalog_table" "cta_exchange_stats_analytics" {
  name          = "${terraform.workspace}_analytics_cta_exchange_stats"
  database_name = var.database_name
  table_type    = "EXTERNAL_TABLE"
  parameters = {
    "projection.startdate.type"          = "date"
    "projection.startdate.range"         = "2020/01/01,NOW"
    "projection.startdate.format"        = "yyyy/MM/dd"
    "projection.startdate.interval"      = 1
    "projection.startdate.interval.unit" = "DAYS"
    "projection.enabled"                 = true
    "storage.location.template"          = "s3://${var.virology_test_stats_bucket_id}/cta-exchange/$${startdate}"

  }
  partition_keys {
    name = "startDate"
    type = "string"
  }

  storage_descriptor {
    location      = "s3://${var.virology_test_stats_bucket_id}/cta-exchange/"
    input_format  = "org.apache.hadoop.mapred.TextInputFormat"
    output_format = "org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat"

    ser_de_info {
      name                  = "json"
      serialization_library = "org.openx.data.jsonserde.JsonSerDe"
    }


    columns {
      name = "testType"
      type = "string"
    }
    columns {
      name = "platform"
      type = "string"
    }
    columns {
      name = "tokenAgeRange"
      type = "string"
    }
    columns {
      name = "source"
      type = "string"
    }
    columns {
      name = "total"
      type = "int"
    }
    columns {
      name = "appVersion"
      type = "string"
    }
  }
}

resource "aws_glue_catalog_table" "cta_token_status_stats_analytics" {
  name          = "${terraform.workspace}_analytics_cta_token_status_stats"
  database_name = var.database_name
  table_type    = "EXTERNAL_TABLE"
  parameters = {
    "projection.startdate.type"          = "date"
    "projection.startdate.range"         = "2020/01/01,NOW"
    "projection.startdate.format"        = "yyyy/MM/dd"
    "projection.startdate.interval"      = 1
    "projection.startdate.interval.unit" = "DAYS"
    "projection.enabled"                 = true
    "storage.location.template"          = "s3://${var.virology_test_stats_bucket_id}/cta-token-status/$${startdate}"

  }
  partition_keys {
    name = "startDate"
    type = "string"
  }

  storage_descriptor {
    location      = "s3://${var.virology_test_stats_bucket_id}/cta-token-status/"
    input_format  = "org.apache.hadoop.mapred.TextInputFormat"
    output_format = "org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat"

    ser_de_info {
      name                  = "json"
      serialization_library = "org.openx.data.jsonserde.JsonSerDe"
    }


    columns {
      name = "testType"
      type = "string"
    }
    columns {
      name = "source"
      type = "string"
    }
    columns {
      name = "total"
      type = "int"
    }
  }
}
