locals {
  table_name = "${terraform.workspace}_analytics_app_store_extended"
}

module "analytics_app_store_extended" {
  source                   = "../../libraries/analytics_s3"
  name                     = "analytics-app-store-extended"
  service                  = var.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  tags                     = var.tags
}

resource "aws_glue_catalog_table" "this" {
  name          = local.table_name
  database_name = var.database_name

  storage_descriptor {
    location      = "s3://${module.analytics_app_store_extended.bucket_name}/"
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
      name = "apple_app_units"
      type = "int"
    }

    columns {
      name = "google_new_users"
      type = "int"
    }

    columns {
      name = "apple_deletions"
      type = "int"
    }

    columns {
      name = "google_user_loss"
      type = "int"
    }

    columns {
      name = "apple_opt_in_rate"
      type = "float"
    }

    columns {
      name = "google_install_base"
      type = "int"
    }

    columns {
      name = "apple_active_last_30_days"
      type = "int"
    }

    columns {
      name = "google_monthly_active_users"
      type = "int"
    }

    columns {
      name = "apple_active_devices"
      type = "int"
    }

    columns {
      name = "google_daily_active_users"
      type = "int"
    }

    columns {
      name = "google_monthly_returning_users"
      type = "int"
    }

    columns {
      name = "google_new_device_acquisitions"
      type = "int"
    }

    columns {
      name = "google_installed_audience"
      type = "int"
    }

    columns {
      name = "additional_metric_1"
      type = "int"
    }

    columns {
      name = "additional_metric_2"
      type = "int"
    }

    columns {
      name = "additional_metric_3"
      type = "int"
    }

    columns {
      name = "additional_metric_4"
      type = "int"
    }

    columns {
      name = "additional_metric_5"
      type = "int"
    }

    columns {
      name = "additional_metric_6"
      type = "int"
    }
  }
}
