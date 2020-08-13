output "role" {
  value = aws_iam_role.lambda_execution_role
}

output "policy" {
  value = aws_iam_role_policy.lambda_execution_policy
}
