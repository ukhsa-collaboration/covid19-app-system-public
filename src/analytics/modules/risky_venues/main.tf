locals {
  table_name = "${terraform.workspace}_analytics_risky_venues"
}

resource "aws_glue_catalog_table" "this" {
  name          = local.table_name
  database_name = var.database_name

  storage_descriptor {
    location      = var.risky_venues_bucket_name
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
      name = "alert_created"
      type = "string"
    }

    columns {
      name = "alert_published"
      type = "string"
    }

    columns {
      name = "alert_approved"
      type = "string"
    }

    columns {
      name = "exposure_start"
      type = "string"
    }

    columns {
      name = "exposure_end"
      type = "string"
    }

    columns {
      name = "venue_id"
      type = "string"
    }

    columns {
      name = "venue_type"
      type = "string"
    }

    columns {
      name = "cases"
      type = "int"
    }

    columns {
      name = "alert_type"
      type = "string"
    }

    columns {
      name = "created_by"
      type = "string"
    }

  }
}

