output "lambda_function_name" {
  value = aws_lambda_function.this.function_name
}

output "lambda_function_arn" {
  value = aws_lambda_function.this.arn
}

output "version" {
  value = aws_lambda_function.this.version
}
