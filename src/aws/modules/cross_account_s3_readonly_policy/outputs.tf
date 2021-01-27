output "policy_document" {
  value = data.aws_iam_policy_document.cross_account_s3_readonly_policy
}
