locals {
  table_name                      = "${terraform.workspace}_analytics_england_deprivation_lookup"
  england_deprivation_lookup_path = abspath("../../../static/england-deprivation-lookup-table.csv")
}

module "england_deprivation_lookup" {
  source                   = "../../libraries/analytics_s3"
  name                     = "analytics-england-deprivation-lookup-table"
  service                  = var.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  enable_versioning        = false
  logs_bucket_id           = var.logs_bucket_id
  tags                     = var.tags
}

resource "aws_s3_bucket_object" "payload" {
  bucket = module.england_deprivation_lookup.bucket_name
  key    = "england-deprivation-lookup-table.csv"
  source = local.england_deprivation_lookup_path
  etag   = filemd5(local.england_deprivation_lookup_path)
}

resource "aws_glue_catalog_table" "this" {
  name          = local.table_name
  database_name = var.database_name

  storage_descriptor {
    location      = "s3://${module.england_deprivation_lookup.bucket_name}/"
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
      name = "lad20cd"
      type = "string"
    }

    columns {
      name = "lad19cd"
      type = "string"
    }

    columns {
      name = "local_authority_2019"
      type = "string"
    }

    columns {
      name = "imd_average_rank"
      type = "float"
    }

    columns {
      name = "imd_rank_of_average_rank"
      type = "int"
    }

    columns {
      name = "imd_average_score"
      type = "float"
    }

    columns {
      name = "imd_rank_of_average_score"
      type = "int"
    }
  }
}
