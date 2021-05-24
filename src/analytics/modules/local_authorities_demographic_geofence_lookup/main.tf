locals {
  table_name                                                = "${terraform.workspace}_analytics_local_authorities_demographic_geographic_lookup"
  analytics_local_authorities_demographic_geographic_lookup = abspath("../../../static/analytics-local-authorities-demographic-geographic-lookup.csv")
}

module "local_authorities_demographic_geographic_lookup" {
  source                   = "../../libraries/analytics_s3"
  name                     = "analytics-local-authorities-demographic-geo-lookup"
  service                  = var.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  tags                     = var.tags
}

resource "aws_s3_bucket_object" "payload" {
  bucket       = module.local_authorities_demographic_geographic_lookup.bucket_name
  key          = "analytics-local-authorities-demographic-geographic-lookup.csv"
  source       = local.analytics_local_authorities_demographic_geographic_lookup
  etag         = filemd5(local.analytics_local_authorities_demographic_geographic_lookup)
  content_type = "text/csv"
}

resource "aws_glue_catalog_table" "this" {
  name          = local.table_name
  database_name = var.database_name

  storage_descriptor {
    location      = "s3://${module.local_authorities_demographic_geographic_lookup.bucket_name}/"
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
      name = "region"
      type = "string"
    }

    columns {
      name = "country"
      type = "string"
    }

    columns {
      name = "local_authority_population"
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
      name = "gor10cd"
      type = "string"
    }

    columns {
      name = "ctry12cd"
      type = "string"
    }
  }
}
