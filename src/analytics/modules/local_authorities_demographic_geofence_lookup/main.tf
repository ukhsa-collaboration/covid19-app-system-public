locals {
  table_name                                                = "${terraform.workspace}_analytics_local_authorities_demographic_geographic_lookup"
  analytics_local_authorities_demographic_geographic_lookup = abspath("../../../static/analytics-local-authorities-demographic-geographic-lookup-20211105.csv")
}

module "local_authorities_demographic_geographic_lookup" {
  source                   = "../../libraries/analytics_s3"
  name                     = "analytics-local-authorities-demographic-geo-lookup"
  service                  = var.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  enable_versioning        = false
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

    columns {
      name = "upper_tier_local_authority"
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
      name = "0_years_age_population"
      type = "int"
    }

    columns {
      name = "1_years_age_population"
      type = "int"
    }

    columns {
      name = "2_years_age_population"
      type = "int"
    }

    columns {
      name = "3_years_age_population"
      type = "int"
    }

    columns {
      name = "4_years_age_population"
      type = "int"
    }

    columns {
      name = "5_years_age_population"
      type = "int"
    }

    columns {
      name = "6_years_age_population"
      type = "int"
    }

    columns {
      name = "7_years_age_population"
      type = "int"
    }

    columns {
      name = "8_years_age_population"
      type = "int"
    }

    columns {
      name = "9_years_age_population"
      type = "int"
    }

    columns {
      name = "10_years_age_population"
      type = "int"
    }

    columns {
      name = "11_years_age_population"
      type = "int"
    }

    columns {
      name = "12_years_age_population"
      type = "int"
    }

    columns {
      name = "13_years_age_population"
      type = "int"
    }

    columns {
      name = "14_years_age_population"
      type = "int"
    }

    columns {
      name = "15_years_age_population"
      type = "int"
    }

    columns {
      name = "16_years_age_population"
      type = "int"
    }

    columns {
      name = "17_years_age_population"
      type = "int"
    }

    columns {
      name = "18_years_age_population"
      type = "int"
    }

    columns {
      name = "19_years_age_population"
      type = "int"
    }

    columns {
      name = "20_years_age_population"
      type = "int"
    }

    columns {
      name = "21_years_age_population"
      type = "int"
    }

    columns {
      name = "22_years_age_population"
      type = "int"
    }

    columns {
      name = "23_years_age_population"
      type = "int"
    }

    columns {
      name = "24_years_age_population"
      type = "int"
    }

    columns {
      name = "25_years_age_population"
      type = "int"
    }

    columns {
      name = "26_years_age_population"
      type = "int"
    }

    columns {
      name = "27_years_age_population"
      type = "int"
    }

    columns {
      name = "28_years_age_population"
      type = "int"
    }

    columns {
      name = "29_years_age_population"
      type = "int"
    }

    columns {
      name = "30_years_age_population"
      type = "int"
    }

    columns {
      name = "31_years_age_population"
      type = "int"
    }

    columns {
      name = "32_years_age_population"
      type = "int"
    }

    columns {
      name = "33_years_age_population"
      type = "int"
    }

    columns {
      name = "34_years_age_population"
      type = "int"
    }

    columns {
      name = "35_years_age_population"
      type = "int"
    }

    columns {
      name = "36_years_age_population"
      type = "int"
    }

    columns {
      name = "37_years_age_population"
      type = "int"
    }

    columns {
      name = "38_years_age_population"
      type = "int"
    }

    columns {
      name = "39_years_age_population"
      type = "int"
    }

    columns {
      name = "40_years_age_population"
      type = "int"
    }

    columns {
      name = "41_years_age_population"
      type = "int"
    }

    columns {
      name = "42_years_age_population"
      type = "int"
    }

    columns {
      name = "43_years_age_population"
      type = "int"
    }

    columns {
      name = "44_years_age_population"
      type = "int"
    }

    columns {
      name = "45_years_age_population"
      type = "int"
    }

    columns {
      name = "46_years_age_population"
      type = "int"
    }

    columns {
      name = "47_years_age_population"
      type = "int"
    }

    columns {
      name = "48_years_age_population"
      type = "int"
    }

    columns {
      name = "49_years_age_population"
      type = "int"
    }

    columns {
      name = "50_years_age_population"
      type = "int"
    }

    columns {
      name = "51_years_age_population"
      type = "int"
    }

    columns {
      name = "52_years_age_population"
      type = "int"
    }

    columns {
      name = "53_years_age_population"
      type = "int"
    }

    columns {
      name = "54_years_age_population"
      type = "int"
    }

    columns {
      name = "55_years_age_population"
      type = "int"
    }

    columns {
      name = "56_years_age_population"
      type = "int"
    }

    columns {
      name = "57_years_age_population"
      type = "int"
    }

    columns {
      name = "58_years_age_population"
      type = "int"
    }

    columns {
      name = "59_years_age_population"
      type = "int"
    }

    columns {
      name = "60_years_age_population"
      type = "int"
    }

    columns {
      name = "61_years_age_population"
      type = "int"
    }

    columns {
      name = "62_years_age_population"
      type = "int"
    }

    columns {
      name = "63_years_age_population"
      type = "int"
    }

    columns {
      name = "64_years_age_population"
      type = "int"
    }

    columns {
      name = "65_years_age_population"
      type = "int"
    }

    columns {
      name = "66_years_age_population"
      type = "int"
    }

    columns {
      name = "67_years_age_population"
      type = "int"
    }

    columns {
      name = "68_years_age_population"
      type = "int"
    }

    columns {
      name = "69_years_age_population"
      type = "int"
    }

    columns {
      name = "70_years_age_population"
      type = "int"
    }

    columns {
      name = "71_years_age_population"
      type = "int"
    }

    columns {
      name = "72_years_age_population"
      type = "int"
    }

    columns {
      name = "73_years_age_population"
      type = "int"
    }

    columns {
      name = "74_years_age_population"
      type = "int"
    }

    columns {
      name = "75_years_age_population"
      type = "int"
    }

    columns {
      name = "76_years_age_population"
      type = "int"
    }

    columns {
      name = "77_years_age_population"
      type = "int"
    }

    columns {
      name = "78_years_age_population"
      type = "int"
    }

    columns {
      name = "79_years_age_population"
      type = "int"
    }

    columns {
      name = "80_years_age_population"
      type = "int"
    }

    columns {
      name = "81_years_age_population"
      type = "int"
    }

    columns {
      name = "82_years_age_population"
      type = "int"
    }

    columns {
      name = "83_years_age_population"
      type = "int"
    }

    columns {
      name = "84_years_age_population"
      type = "int"
    }

    columns {
      name = "85_years_age_population"
      type = "int"
    }

    columns {
      name = "86_years_age_population"
      type = "int"
    }

    columns {
      name = "87_years_age_population"
      type = "int"
    }

    columns {
      name = "88_years_age_population"
      type = "int"
    }

    columns {
      name = "89_years_age_population"
      type = "int"
    }

    columns {
      name = "90_plus_years_age_population"
      type = "int"
    }

    columns {
      name = "16_plus_years_age_population"
      type = "int"
    }

    columns {
      name = "16_minus_years_age_population"
      type = "int"
    }

  }
}
