locals {
  table_name = "${terraform.workspace}_analytics_apple_sales_report"
}

module "analytics_apple_sales_report" {
  source                   = "../../libraries/analytics_s3"
  name                     = "analytics-apple-sales-report"
  service                  = var.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  enable_versioning        = true
  logs_bucket_id           = var.logs_bucket_id
  tags                     = var.tags
}

resource "aws_glue_catalog_table" "this" {
  name          = local.table_name
  database_name = var.database_name

  storage_descriptor {
    location      = "s3://${terraform.workspace}-analytics-apple-sales-report/"
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
      name = "provider"
      type = "string"
    }

    columns {
      name = "provider_country"
      type = "string"
    }

    columns {
      name = "sku"
      type = "string"
    }

    columns {
      name = "developer"
      type = "string"
    }

    columns {
      name = "title"
      type = "string"
    }

    columns {
      name = "version"
      type = "string"
    }

    columns {
      name = "product_type_identifier"
      type = "string"
    }

    columns {
      name = "units"
      type = "int"
    }

    columns {
      name = "developer_proceeds"
      type = "float"
    }

    columns {
      name = "begin_date"
      type = "string"
    }

    columns {
      name = "end_date"
      type = "string"
    }

    columns {
      name = "customer_currency"
      type = "string"
    }

    columns {
      name = "country_code"
      type = "string"
    }

    columns {
      name = "currency_of_proceeds"
      type = "string"
    }

    columns {
      name = "apple_identifier"
      type = "string"
    }

    columns {
      name = "customer_price"
      type = "float"
    }

    columns {
      name = "promo_code"
      type = "string"
    }

    columns {
      name = "parent_identifier"
      type = "string"
    }

    columns {
      name = "subscription"
      type = "string"
    }

    columns {
      name = "period"
      type = "string"
    }

    columns {
      name = "category"
      type = "string"
    }

    columns {
      name = "cmb"
      type = "string"
    }

    columns {
      name = "device"
      type = "string"
    }

    columns {
      name = "supported_platforms"
      type = "string"
    }

    columns {
      name = "proceeds_reason"
      type = "string"
    }

    columns {
      name = "preserved_pricing"
      type = "string"
    }

    columns {
      name = "client"
      type = "string"
    }

    columns {
      name = "order_type"
      type = "string"
    }
  }
}
