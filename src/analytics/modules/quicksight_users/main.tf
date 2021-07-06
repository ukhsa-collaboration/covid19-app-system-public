locals {
  table_name = "${terraform.workspace}_analytics_quicksight_users"
}

module "quicksight_users" {
  source                   = "../../libraries/analytics_s3"
  name                     = "analytics-quicksight-users"
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
    location      = "s3://${module.quicksight_users.bucket_name}/"
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
      name = "active"
      type = "boolean"
    }

    columns {
      name = "arn"
      type = "string"
    }

    columns {
      name = "custom_permissions_name"
      type = "string"
    }

    columns {
      name = "email"
      type = "string"
    }

    columns {
      name = "identity_type"
      type = "string"
    }

    columns {
      name = "principal_id"
      type = "string"
    }

    columns {
      name = "role"
      type = "string"
    }

    columns {
      name = "user_name"
      type = "string"
    }
  }
}
