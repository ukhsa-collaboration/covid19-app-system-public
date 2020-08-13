variable "s3_input_bucket" {
  description = "The S3 bucket that is used to serve content"
}
variable "s3_output_bucket" {
  description = "The S3 bucket that is used to receive the content"
}
variable "table_name" {
  description = "The name of the table to be created"
}
variable "database_name" {
  description = "The name of the database to be created"
}