resource "aws_cloudwatch_dashboard" "analytics_dashboard" {
  dashboard_name = "${var.env}-analytics-errors-warnings"
  dashboard_body = templatefile("${path.module}/templates/analytics_dashboard.tmpl", {
    advanced_analytics_function = var.advanced_analytics_function
  })
}
