resource "aws_glue_catalog_table" "analytics_mobile_events" {
  name          = var.glue_table_name
  database_name = var.glue_db_name

  table_type = "EXTERNAL_TABLE"

  parameters = {
    EXTERNAL                                     = "TRUE"
    "projection.submitteddatehour.type"          = "date"
    "projection.submitteddatehour.range"         = "2020/01/01/00,NOW"
    "projection.submitteddatehour.format"        = "yyyy/MM/dd/HH"
    "projection.submitteddatehour.interval"      = 1
    "projection.submitteddatehour.interval.unit" = "HOURS"
    "projection.enabled"                         = true
    "storage.location.template"                  = "s3://${var.analytics_submission_events_bucket_id}/$${submitteddatehour}"
  }

  partition_keys {
    name = "submitteddatehour"
    type = "string"
  }

  storage_descriptor {
    location      = "s3://${var.analytics_submission_events_bucket_id}/"
    input_format  = "org.apache.hadoop.mapred.TextInputFormat"
    output_format = "org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat"

    ser_de_info {
      name                  = "json"
      serialization_library = "org.openx.data.jsonserde.JsonSerDe"
    }

    columns {
      name = "uuid"
      type = "string"
    }
    columns {
      name = "metadata"
      type = "struct<operatingSystemVersion:string,latestApplicationVersion:string,deviceModel:string,postalDistrict:string,localAuthority:string>"
    }
    columns {
      name = "events"
      type = "array<struct<type:string,version:int,payload:struct<date:string,infectiousness:string,scanInstances:array<struct<minimumAttenuation:int,secondsSinceLastScan:int,typicalAttenuation:int>>,riskScore:string>>>"
    }
  }
}

resource "aws_athena_named_query" "analytics_mobile_events_query" {
  name      = var.glue_table_name
  database  = var.glue_db_name
  workgroup = var.workgroup_name
  query     = <<EOF
    SELECT * FROM "${var.glue_table_name}"
  EOF
}
