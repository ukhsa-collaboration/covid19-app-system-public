locals {
  table_name                           = "${terraform.workspace}_analytics_local_authorities_geofence"
  local_authorities_geofence_file_path = "${path.module}/static/analytics-local-authorities-geofence.csv"
}

module "local_authorities_geofence" {
  source                   = "../../libraries/analytics_s3"
  name                     = "analytics-local-authorities-geofence"
  service                  = var.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
}

resource "aws_s3_bucket_object" "payload" {
  bucket       = module.local_authorities_geofence.bucket_name
  key          = "analytics-local-authorities-geofence.csv"
  source       = local.local_authorities_geofence_file_path
  etag         = filemd5(local.local_authorities_geofence_file_path)
  content_type = "text/csv"
}

resource "aws_glue_catalog_table" "this" {
  name          = local.table_name
  database_name = var.database_name

  storage_descriptor {
    location      = "s3://${module.local_authorities_geofence.bucket_name}/"
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
      name = "local_authority"
      type = "string"
    }

    columns {
      name = "latitude"
      type = "float"
    }

    columns {
      name = "longitude"
      type = "float"
    }

  }
}
