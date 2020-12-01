variable "athena_output_store" {
  description = "The name of the bucket where Athena query results are stored"
}

variable "name" {
  description = "The name you want to use to identify this workgroup in Athena."
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}