variable "database_name" {
  description = "The name of the database this module should create"
}

variable "workgroup_name" {
  description = "The name of the workspace associated to the database"
}

variable "analytics_submission_store_parquet_bucket_id" {
  description = "The name of the bucket the stores mobile analytics"
}

variable "analytics_submission_store_consolidated_parquet_bucket_id" {
  description = "The name of the bucket the stores mobile analytics"
}