variable "glue_table_name" {
  description = "The name of the glue table this module should create"
}

variable "glue_db_name" {
  description = "Name of the glue database."
}

variable "workgroup_name" {
  description = "Athena workgroup name"
}

variable "analytics_submission_events_bucket_id" {
  description = "The name of the S3 bucket the stores mobile events analytics"
}
