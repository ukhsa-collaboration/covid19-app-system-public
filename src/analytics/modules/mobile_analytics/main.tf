locals {
  table_name = "${terraform.workspace}_analytics_mobile"
}

resource "aws_athena_named_query" "this" {
  name      = local.table_name
  database  = var.database_name
  workgroup = var.workgroup_name
  query     = <<EOF
    SELECT * FROM "${local.table_name}"
    SELECT
      "substring"("startdate", 1, 10) "truncatedstartdate"
    , "substring"("enddate", 1, 10) "truncatedenddate"
    , *
    FROM "${local.table_name}"
  EOF
}
