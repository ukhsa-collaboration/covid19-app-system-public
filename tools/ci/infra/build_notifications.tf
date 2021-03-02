resource "aws_cloudwatch_event_rule" "build_state_change" {
  name        = "build-state-change-events-${each.key}"
  description = "Capture the build state change events for deployments"
  for_each    = toset(var.target_environments)

  event_pattern = <<EOF
{
  "source": [ 
    "aws.codebuild"
  ], 
  "detail-type": [
    "CodeBuild Build State Change"
  ],
  "detail": {
    "build-status": [
      "SUCCEEDED", 
      "FAILED",
      "STOPPED" 
    ],
    "project-name": [
      "deploy-cta-${each.key}",
      "deploy-tier-metadata-${each.key}",
      "deploy-analytics-${each.key}",
      "deploy-pubdash-${each.key}"
    ]
  }  
}  
EOF
}

resource "aws_cloudwatch_event_target" "target_sns_topic" {
  for_each = toset(var.target_environments)
  rule     = aws_cloudwatch_event_rule.build_state_change[each.key].name
  arn      = var.deploy_events_sns_arn
}
resource "aws_cloudwatch_event_rule" "build_failure_notify_events" {
  name        = "build-failure-notify-events"
  description = "Capture the build failures in CI and PR"
  count       = var.allow_dev_pipelines == true ? 1 : 0

  event_pattern = <<EOF
{
  "source": [ 
    "aws.codebuild"
  ], 
  "detail-type": [
    "CodeBuild Build State Change"
  ],
  "detail": {
    "build-status": [
      "FAILED"
    ],
    "project-name": [
      "ci-app-system",
      "pr-app-system",
      "resources-cleanup"
    ]
  }  
}  
EOF
}
resource "aws_cloudwatch_event_target" "target_sns_topic_ci" {
  count = var.allow_dev_pipelines == true ? 1 : 0
  rule  = aws_cloudwatch_event_rule.build_failure_notify_events[0].name
  arn   = var.build_failure_events_sns_arn
}
