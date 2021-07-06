locals {
  table_name                                     = "${terraform.workspace}_analytics_postcode_demographic_geographic_lookup_v2"
  postcode_demographic_geographic_lookup_path_v2 = abspath("../../../static/analytics-postcode-demographic-geographic-lookup-v2.csv")
}

module "postcode_demographic_geographic_lookup_v2" {
  source                   = "../../libraries/analytics_s3"
  name                     = "analytics-postcode-demographic-geo-lookup-v2"
  service                  = var.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  enable_versioning        = false
  logs_bucket_id           = var.logs_bucket_id
  tags                     = var.tags
}

resource "aws_s3_bucket_object" "payload" {
  bucket       = module.postcode_demographic_geographic_lookup_v2.bucket_name
  key          = "analytics-postcode-demographic-geographic-lookup-v2.csv"
  source       = local.postcode_demographic_geographic_lookup_path_v2
  etag         = filemd5(local.postcode_demographic_geographic_lookup_path_v2)
  content_type = "text/csv"
}

resource "aws_glue_catalog_table" "this" {
  name          = local.table_name
  database_name = var.database_name

  storage_descriptor {
    location      = "s3://${module.postcode_demographic_geographic_lookup_v2.bucket_name}/"
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

    columns {
      name = "upper_tier_local_authority"
      type = "string"
    }

    columns {
      name = "utla20cd"
      type = "string"
    }

    columns {
      name = "gor10cd"
      type = "string"
    }

    columns {
      name = "ctry12cd"
      type = "string"
    }

    columns {
      name = "local_authority_welsh"
      type = "string"
    }

    columns {
      name = "upper_tier_local_authority_welsh"
      type = "string"
    }

    columns {
      name = "region_welsh"
      type = "string"
    }

    columns {
      name = "country_welsh"
      type = "string"
    }

    columns {
      name = "mapping_version"
      type = "int"
    }


  }
}
