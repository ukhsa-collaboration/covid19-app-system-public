locals {
  table_name                = "${terraform.workspace}_analytics_coronavirus_gov_public_data_lookup_${var.country}"
  full_postcode_lookup_path = abspath("../../../static/government-data-lookup-nation-${var.country}.csv")
}

module "coronavirus_gov_public_data_lookup" {
  source                   = "../../libraries/analytics_s3"
  name                     = "analytics-coronavirus-gov-public-data-lookup-${var.country}"
  service                  = var.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  logs_bucket_id           = var.logs_bucket_id
  tags                     = var.tags
}

resource "aws_glue_catalog_table" "this" {
  name          = local.table_name
  database_name = var.database_name

  storage_descriptor {
    location      = "s3://${module.coronavirus_gov_public_data_lookup.bucket_name}/"
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
      name = "date"
      type = "string"
    }

    columns {
      name = "areaName"
      type = "string"
    }

    columns {
      name = "newCasesByPublishDate"
      type = "int"
    }

    columns {
      name = "newCasesBySpecimenDate"
      type = "int"
    }

    columns {
      name = "newCasesLFDConfirmedPCRBySpecimenDate"
      type = "int"
    }

    columns {
      name = "newCasesLFDOnlyBySpecimenDate"
      type = "int"
    }

    columns {
      name = "newCasesPCROnlyBySpecimenDate"
      type = "int"
    }

    columns {
      name = "newLFDTests"
      type = "int"
    }

    columns {
      name = "newPCRTestsByPublishDate"
      type = "int"
    }

    columns {
      name = "newTestsByPublishDate"
      type = "int"
    }

    columns {
      name = "newVirusTests"
      type = "int"
    }

  }
}
