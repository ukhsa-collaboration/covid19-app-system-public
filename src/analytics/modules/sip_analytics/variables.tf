variable "database_name" {
  description = "The name of the database this module should create"
  type        = string
}

variable "location" {
  description = "Location of the target S3 bucket"
  type        = string
}

variable "workgroup_name" {
  description = "The name of the workspace associated to the database"
}
