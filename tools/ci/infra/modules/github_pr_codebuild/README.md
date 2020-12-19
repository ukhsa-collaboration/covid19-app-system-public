# CodeBuild project for a GitHub repository

This module represents a CodeBuild project using a docker image hosted in AWS ECR and a GitHub repository.

It requires a pre-configured IAM role that has all the access rights required by the tasks performed in the project and additionally the following:

1. AWS ECR access
1. The rights required by AWS CodeBuild (CloudWatch and S3 access)
1. ```secretsmanager:GetSecretValue```
