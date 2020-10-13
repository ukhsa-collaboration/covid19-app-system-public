locals {
  store_table_name           = "${terraform.workspace}_analytics_demographics_data"
  demographic_data_file_path = "${path.module}/static/demographic_data.csv"
}

module "analytics_demographics_data_s3" {
  source                   = "../../libraries/analytics_s3"
  name                     = "analytics-demographic-data"
  service                  = var.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
}

resource "aws_s3_bucket_object" "payload" {
  bucket       = module.analytics_demographics_data_s3.bucket_name
  key          = "demographic_data.csv"
  source       = local.demographic_data_file_path
  etag         = filemd5(local.demographic_data_file_path)
  content_type = "text/csv"
}

resource "aws_glue_catalog_table" "demographic_data_table" {
  name          = local.store_table_name
  database_name = var.database_name

  storage_descriptor {
    location      = "s3://${module.analytics_demographics_data_s3.bucket_name}/"
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
      name = "postal district"
      type = "string"
    }

    columns {
      name = "population"
      type = "int"
    }

  }

}