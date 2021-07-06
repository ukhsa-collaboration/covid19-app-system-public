locals {
  table_name                = "${terraform.workspace}_analytics_full_postcode_lookup"
  full_postcode_lookup_path = abspath("../../../static/analytics-full-postcode-lookup.csv.gz")
}

module "full_postcode_lookup" {
  source                   = "../../libraries/analytics_s3"
  name                     = "analytics-full-postcode-lookup"
  service                  = var.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  enable_versioning        = false
  logs_bucket_id           = var.logs_bucket_id
  tags                     = var.tags
}

resource "aws_s3_bucket_object" "payload" {
  bucket = module.full_postcode_lookup.bucket_name
  key    = "analytics-full-postcode-lookup.csv.gz"
  source = local.full_postcode_lookup_path
  etag   = filemd5(local.full_postcode_lookup_path)
}

resource "aws_glue_catalog_table" "this" {
  name          = local.table_name
  database_name = var.database_name

  storage_descriptor {
    location      = "s3://${module.full_postcode_lookup.bucket_name}/"
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
      name = "postcodeNoSpace"
      type = "string"
    }

    columns {
      name = "dateOfIntroduction"
      type = "string"
    }

    columns {
      name = "dateOfTermination"
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

    columns {
      name = "lad20cd"
      type = "string"
    }

    columns {
      name = "localAuthorityName"
      type = "string"
    }

    columns {
      name = "localAuthorityNameWelsh"
      type = "string"
    }

    columns {
      name = "utla20cd"
      type = "string"
    }

    columns {
      name = "upperTierLocalAuthorityName"
      type = "string"
    }

    columns {
      name = "upperTierLocalAuthorityNameWelsh"
      type = "string"
    }

    columns {
      name = "gor10cd"
      type = "string"
    }

    columns {
      name = "regionName"
      type = "string"
    }

    columns {
      name = "regionNameWelsh"
      type = "string"
    }

    columns {
      name = "ctry12cd"
      type = "string"
    }

    columns {
      name = "countryName"
      type = "string"
    }

    columns {
      name = "countryNameWelsh"
      type = "string"
    }
  }
}
