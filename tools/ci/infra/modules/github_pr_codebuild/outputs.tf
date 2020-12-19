output "project_name" {
  description = " The name of the CodeBuild project"
  value       = var.name
}

output "webhook_url" {
  description = "The URL for the webhook that receives version control events"
  value       = aws_codebuild_webhook.this.payload_url
}
