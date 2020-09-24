locals {
  table_name = "${terraform.workspace}_analytics_risky_post_districts"
}

resource "aws_glue_catalog_table" "this" {
  name          = local.table_name
  database_name = var.database_name

  storage_descriptor {
    location      = "s3://${var.risky_post_codes_bucket_id}/raw/"
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
      name = "postal_district_code"
      type = "string"
    }

    columns {
      name = "risk_indicator"
      type = "string"
    }
  }
}

resource "aws_athena_named_query" "this" {
  name      = local.table_name
  database  = var.database_name
  workgroup = var.workgroup_name
  query     = <<EOF
    SELECT * FROM "${local.table_name}"
  EOF
}