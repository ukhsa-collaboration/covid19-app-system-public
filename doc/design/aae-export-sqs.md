# AAE Export: Pattern "Decouple S3 Ingest and Export via SQS"

## Overview
![AAE Export](diagrams/aae-export-sqs.png)

## Flow
- Analytics Events Submission lambda receives the analytics payload from mobile apps and stores it in S3
- An EventBridge rule notifies SQS of the new json file stored in S3
- A new event is created in SQS which is delivered to the AAE export lambda 
- AAE export lambda processes the event, gets the corresponding json from S3, sends it to AAE and finally removes the event it from SQS 
- If there is a failure when processing the event, it will re-appear in SQS to be re-processed later 
- After a number of failed attempts the event will be added to the dead letter queue (DLQ)

## Configuration 
* Use EventBridge (& CloudTrail) to automatically add events to SQS after files are added to S3 (```aws_cloudwatch_event_rule``` and ```aws_cloudwatch_event_target```)
* Let a Lambda function automatically process these events (```aws_lambda_event_source_mapping```)
* Control retry behavior in Lambda by re-throwing (retry) or catching & logging (no retry) Java Exceptions 
* Control retry behavior by controlling the pause between export attempts (```aws_sqs_queue.visibility_timeout_seconds```) and the number of retries (```aws_sqs_queue.redrive_policy.maxReceiveCount```)
* Dead Letter Queue for files that cannot be exported (```aws_sqs_queue.redrive_policy.deadLetterTargetArn```)
* Target-environment-specific feature flag to enable/disable SQS message processing (e.g. ```aws_lambda_event_source_mapping.enabled = contains(var.enabled_workspaces, "*") || contains(var.enabled_workspaces, terraform.workspace) || contains(var.enabled_workspaces, "branch") && substr(terraform.workspace, 0, 3) != "te-"```). This allows us to pause export to AAE for up to 14 days without "losing" files added to S3
* Alert is triggered when ```ApproximateNumberOfMessagesVisible > x```
