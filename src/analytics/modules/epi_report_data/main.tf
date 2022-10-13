locals {
  table_name = "${terraform.workspace}_epi_report_data"
}

module "epi_report_data" {
  source                   = "../../libraries/analytics_s3"
  name                     = "analytics-epi-metric-data"
  service                  = var.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  enable_versioning        = false
  logs_bucket_id           = var.logs_bucket_id
  tags                     = var.tags
}

resource "aws_s3_bucket_object" "empty_file" {
  bucket       = module.epi_report_data.bucket_name
  key          = "emptyfile"
  source       = abspath("../../../static/empty")
  etag         = filemd5("../../../static/empty")
  content_type = "text/plain"
}

resource "aws_glue_catalog_table" "this" {
  name          = local.table_name
  database_name = var.database_name

  storage_descriptor {
    location      = "s3://${module.epi_report_data.bucket_name}/"
    input_format  = "org.apache.hadoop.mapred.TextInputFormat"
    output_format = "org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat"

    ser_de_info {
      name                  = "csv"
      serialization_library = "org.apache.hadoop.hive.serde2.OpenCSVSerde"

      parameters = {
        "separatorChar"          = ","
        "skip.header.line.count" = "1"
      }
    }

    columns {
      name = "date"
      type = "string"
    }
    columns {
      name = "notifications"
      type = "int"
    }
    columns {
      name = "cases_directly_averted_perfect"
      type = "float"
    }
    columns {
      name = "cases_directly_averted_80_percent"
      type = "float"
    }
    columns {
      name = "cases_directly_averted_60_percent"
      type = "float"
    }
    columns {
      name = "cases_directly_averted_40_percent"
      type = "float"
    }
    columns {
      name = "cases_directly_averted_20_percent"
      type = "float"
    }
  }
}
