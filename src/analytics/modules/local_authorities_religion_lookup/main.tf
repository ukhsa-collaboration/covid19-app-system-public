locals {
  table_name                             = "${terraform.workspace}_analytics_local_authorities_religion_lookup"
  local_authorities_religion_lookup_path = abspath("../../../static/analytics-local-authorities-religion-lookup.csv")
}

module "local_authorities_religion_lookup" {
  source                   = "../../libraries/analytics_s3"
  name                     = "analytics-local-authorities-religion-lookup"
  service                  = var.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  enable_versioning        = false
  logs_bucket_id           = var.logs_bucket_id
  tags                     = var.tags
}

resource "aws_s3_bucket_object" "payload" {
  bucket       = module.local_authorities_religion_lookup.bucket_name
  key          = "analytics-local-authorities-religion-lookup.csv"
  source       = local.local_authorities_religion_lookup_path
  etag         = filemd5(local.local_authorities_religion_lookup_path)
  content_type = "text/csv"
}

resource "aws_glue_catalog_table" "this" {
  name          = local.table_name
  database_name = var.database_name

  storage_descriptor {
    location      = "s3://${module.local_authorities_religion_lookup.bucket_name}/"
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
      name = "local_authority_pre_2015"
      type = "string"
    }

    columns {
      name = "religion"
      type = "string"
    }

    columns {
      name = "population"
      type = "int"
    }

  }
}
