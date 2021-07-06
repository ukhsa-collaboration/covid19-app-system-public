locals {
  store_table_name      = "${terraform.workspace}_analytics_app_store"
  qr_posters_table_name = "${terraform.workspace}_analytics_qr_posters"
}

module "analytics_app_store_qr_posters_s3" {
  source                   = "../../libraries/analytics_s3"
  name                     = "analytics-app-store-qr-posters"
  service                  = var.service
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  enable_versioning        = false
  logs_bucket_id           = var.logs_bucket_id
  tags                     = var.tags
}

resource "aws_glue_catalog_table" "mobile_analytics_table" {
  name          = local.store_table_name
  database_name = var.database_name

  storage_descriptor {
    location      = "s3://${module.analytics_app_store_qr_posters_s3.bucket_name}/app-store/"
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
      type = "string" #FIXME
    }

    columns {
      name = "platform"
      type = "string"
    }

    columns {
      name = "average_rating"
      type = "float"
    }

    columns {
      name = "downloads"
      type = "int"
    }

    columns {
      name = "deletes"
      type = "int"
    }

    columns {
      name = "opt_in_proportion"
      type = "float"
    }

  }

}

resource "aws_athena_named_query" "mobile_analytics_query" {
  name      = local.store_table_name
  database  = var.database_name
  workgroup = var.workgroup_name
  query     = <<EOF
    SELECT * FROM "${local.store_table_name}"
  EOF
}


resource "aws_glue_catalog_table" "posters_table" {
  name          = local.qr_posters_table_name
  database_name = var.database_name

  storage_descriptor {
    location      = var.qr_posters_bucket_location
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
      name = "Created"
      type = "string" #FIXME
    }

    columns {
      name = "LocationName"
      type = "string"
    }

    columns {
      name = "PostCode"
      type = "string"
    }

    columns {
      name = "AddressLine1"
      type = "string"
    }

    columns {
      name = "AddressLine2"
      type = "string"
    }

    columns {
      name = "AddressLine3"
      type = "string"
    }

    columns {
      name = "County"
      type = "string"
    }

    columns {
      name = "TownOrCity"
      type = "string"
    }

    columns {
      name = "VenueTypeId"
      type = "string"
    }

    columns {
      name = "Locale"
      type = "string"
    }

  }

}

resource "aws_athena_named_query" "posters_query" {
  name      = local.qr_posters_table_name
  database  = var.database_name
  workgroup = var.workgroup_name
  query     = <<EOF
    SELECT * FROM "${local.qr_posters_table_name}"
  EOF
}
