locals {
  table_name = "${terraform.workspace}_analytics_ios_historical_ratings"
}

module "ios_historical_ratings" {
  source                   = "../../libraries/analytics_s3"
  name                     = "analytics-ios-historical-ratings"
  service                  = var.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  enable_versioning        = false
  logs_bucket_id           = var.logs_bucket_id
  tags                     = var.tags
}

resource "aws_glue_catalog_table" "this" {
  name          = local.table_name
  database_name = var.database_name

  storage_descriptor {
    location      = "s3://${module.ios_historical_ratings.bucket_name}/"
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
      name = "ratingsTotalCount"
      type = "int"
    }

    columns {
      name = "average"
      type = "double"
    }

    columns {
      name = "ratings5starCount"
      type = "int"
    }

    columns {
      name = "ratings4starCount"
      type = "int"
    }

    columns {
      name = "ratings3starCount"
      type = "int"
    }

    columns {
      name = "ratings2starCount"
      type = "int"
    }

    columns {
      name = "ratings1starCount"
      type = "int"
    }
  }
}
