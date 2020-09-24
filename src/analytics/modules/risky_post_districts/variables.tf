variable "database_name" {
  description = "The name of the database this module should create"
}

variable "workgroup_name" {
  description = "The name of the workspace associated to the database"
}

variable "risky_post_codes_bucket_id" {
  description = "The name of the bucket the stores risky post codes"
}