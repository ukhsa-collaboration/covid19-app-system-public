resource "aws_glue_catalog_table" "this" {
  name          = "${terraform.workspace}_analytics_sip"
  database_name = var.database_name

  storage_descriptor {
    location      = var.location
    input_format  = "org.apache.hadoop.mapred.TextInputFormat"
    output_format = "org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat"

    ser_de_info {
      name                  = "json"
      serialization_library = "org.openx.data.jsonserde.JsonSerDe"
    }

    columns {
      name = "event"
      type = "string"
    }

    columns {
      name = "metadata"
      type = "struct<type:string,timestamp:string>"
    }
  }
}
