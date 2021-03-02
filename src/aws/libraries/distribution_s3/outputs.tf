output "bucket" {
  value = aws_s3_bucket.this
}

output "bucket_id" {
  value = aws_s3_bucket.this.id
}
