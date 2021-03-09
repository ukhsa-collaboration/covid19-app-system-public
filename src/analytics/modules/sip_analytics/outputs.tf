output "sip_analytics_create_daily_aggregate_table_query_id" {
  value       = aws_athena_named_query.sip_analytics_create_daily_aggregate_table.id
  description = "The named query ID for the query that creates the table for Sip daily aggregates"
}
