output "database_name" {
  value = aws_athena_database.this.name
}

output "output_bucket_name" {
  value = aws_athena_database.this.bucket
}

output "workgroup_name" {
  value = aws_athena_workgroup.this.name
}