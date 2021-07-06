locals {
  table_name = "${terraform.workspace}_analytics_vaccinations_england"
}

module "analytics_vaccinations_england" {
  source                   = "../../libraries/analytics_s3"
  name                     = "analytics-vaccinations-england"
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
    location      = "s3://${module.analytics_vaccinations_england.bucket_name}/"
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
      name = "publication_date"
      type = "string"
    }

    columns {
      name = "date_range_start"
      type = "string"
    }

    columns {
      name = "date_range_end"
      type = "string"
    }

    columns {
      name = "lad20cd"
      type = "string"
    }

    columns {
      name = "local_authority"
      type = "string"
    }

    columns {
      name = "age_range"
      type = "string"
    }

    columns {
      name = "at_least_dose_1_cumulative"
      type = "int"
    }

    columns {
      name = "dose_1_cumulative"
      type = "int"
    }

    columns {
      name = "dose_2_cumulative"
      type = "int"
    }
  }
}
