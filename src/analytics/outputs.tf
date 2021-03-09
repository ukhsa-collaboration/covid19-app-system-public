output "sip_analytics_create_daily_aggregate_table_query_id" {
  value       = module.sip_analytics.sip_analytics_create_daily_aggregate_table_query_id
  description = "The named query ID for the query that creates the table for Sip daily aggregates"
}

output "sip_analytics_database" {
  value       = aws_glue_catalog_database.this.name
  description = "The database name for the SipGateway analytics"
}

output "analytics_workgroup" {
  value       = module.workgroup.name
  description = "The name given to for all of the QuickSight analytics in Athena"
}
