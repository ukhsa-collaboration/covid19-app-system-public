output "bucket" {
  value = aws_s3_bucket.this
}

output "policy_document" {
  value = data.aws_iam_policy_document.this
}

output "bucket_id" {
  value = aws_s3_bucket.this.id
}