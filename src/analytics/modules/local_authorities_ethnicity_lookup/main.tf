locals {
  table_name                              = "${terraform.workspace}_analytics_local_authorities_ethnicity_lookup"
  local_authorities_ethnicity_lookup_path = abspath("../../../static/local-authority-ethnicity-lookup.csv")
}

module "local_authorities_ethnicity_lookup" {
  source                   = "../../libraries/analytics_s3"
  name                     = "analytics-local-authorities-ethnicity-lookup"
  service                  = var.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  tags                     = var.tags
}

resource "aws_s3_bucket_object" "payload" {
  bucket = module.local_authorities_ethnicity_lookup.bucket_name
  key    = "local-authority-ethnicity-lookup.csv"
  source = local.local_authorities_ethnicity_lookup_path
  etag   = filemd5(local.local_authorities_ethnicity_lookup_path)
}

resource "aws_glue_catalog_table" "this" {
  name          = local.table_name
  database_name = var.database_name

  storage_descriptor {
    location      = "s3://${module.local_authorities_ethnicity_lookup.bucket_name}/"
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
      name = "ethnic_group"
      type = "string"
    }

    columns {
      name = "ethnicity"
      type = "string"
    }

    columns {
      name = "population"
      type = "float"
    }
  }
}
