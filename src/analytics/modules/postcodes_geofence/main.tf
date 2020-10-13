locals {
  table_name                   = "${terraform.workspace}_analytics_postcodes_geofence"
  postcodes_geofence_file_path = "${path.module}/static/analytics-postcodes-geofence.csv"
}

module "postcodes_geofence" {
  source                   = "../../libraries/analytics_s3"
  name                     = "analytics-postcodes-geofence"
  service                  = var.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
}

resource "aws_s3_bucket_object" "payload" {
  bucket       = module.postcodes_geofence.bucket_name
  key          = "analytics-postcodes-geofence.csv"
  source       = local.postcodes_geofence_file_path
  etag         = filemd5(local.postcodes_geofence_file_path)
  content_type = "text/csv"
}

resource "aws_glue_catalog_table" "this" {
  name          = local.table_name
  database_name = var.database_name

  storage_descriptor {
    location      = "s3://${module.postcodes_geofence.bucket_name}/"
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
      name = "postcode"
      type = "string"
    }

    columns {
      name = "local_authority"
      type = "string"
    }

    columns {
      name = "country"
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

resource "aws_athena_named_query" "postcodes_geofence_query" {
  name      = local.table_name
  database  = var.database_name
  workgroup = var.workgroup_name
  query     = <<EOF
SELECT * FROM "${local.table_name}"
  EOF
}
