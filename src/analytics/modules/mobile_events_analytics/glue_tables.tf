################################################################################
# THIS FILE IS AUTO-GENERATED, DO NOT EDIT DIRECTLY
# CHANGES TO TEMPLATE SHOULD BE MADE VIA: tools/templates/glue_tables_events_quicksight.tf.erb
# TO ADD NEW FIELDS, EDIT src/aws/analytics_fields/fields.json
################################################################################

resource "aws_glue_catalog_table" "analytics_mobile_events_parquet" {
  name          = "${var.glue_table_name}_parquet"
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
    "storage.location.template"                  = "s3://${var.analytics_submission_events_parquet_bucket_id}/$${submitteddatehour}"
  }

  partition_keys {
    name = "submitteddatehour"
    type = "string"
  }

  storage_descriptor {
    location      = "s3://${var.analytics_submission_events_parquet_bucket_id}/"
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
