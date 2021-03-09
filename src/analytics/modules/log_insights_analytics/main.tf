
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
      name = "numberOfKeys"
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
      name = "numberOfKeys"
      type = "int"
    }
  }
}

