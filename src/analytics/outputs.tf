output "sip_analytics_database" {
  value       = aws_glue_catalog_database.this.name
  description = "The database name for the SipGateway analytics"
}

output "analytics_workgroup" {
  value       = module.workgroup.name
  description = "The name given to for all of the QuickSight analytics in Athena"
}

output "archive_bucket" {
  value       = module.archive_store.bucket_name
  description = "The bucket name of the archive bucket used by the analytics team:q!"
}
