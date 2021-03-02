locals {
  table_name                                  = "${terraform.workspace}_analytics_postcode_demographic_geographic_lookup"
  postcode_demographic_geographic_lookup_path = abspath("../../../static/analytics-postcode-demographic-geographic-lookup.csv")
}

module "postcode_demographic_geographic_lookup" {
  source                   = "../../libraries/analytics_s3"
  name                     = "analytics-postcode-demographic-geographic-lookup"
  service                  = var.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  tags                     = var.tags
}

resource "aws_s3_bucket_object" "payload" {
  bucket       = module.postcode_demographic_geographic_lookup.bucket_name
  key          = "analytics-postcode-demographic-geographic-lookup.csv"
  source       = local.postcode_demographic_geographic_lookup_path
  etag         = filemd5(local.postcode_demographic_geographic_lookup_path)
  content_type = "text/csv"
}

resource "aws_glue_catalog_table" "this" {
  name          = local.table_name
  database_name = var.database_name

  storage_descriptor {
    location      = "s3://${module.postcode_demographic_geographic_lookup.bucket_name}/"
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
      name = "region"
      type = "string"
    }

    columns {
      name = "country"
      type = "string"
    }

    columns {
      name = "merged_postcode_district_population"
      type = "int"
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

  }
}
