# Isolation Payment

## Overview

![NHS COVIS-19 App and Isolation Payment Gateway](diagrams/isolation-payment.png)

## HTTP API

Mobile-facing submission: see [HTTP API contract](../architecture/api-contracts/isolation-payment-mobile.md)

## Lambda API 

Lambda functions directly exposed to external system in different AWS account (synchronous invocation): see [Lambda API contract](../architecture/api-contracts/isolation-payment-gateway.md)

### Authentication/Authorization

AWS IAM (Cross account):
* Exposing account: AWS account of the "NHS COVIS-19 App"
* External account: AWS account of the "Isolation Payment Gateway"
* Exposing account offers IAM Role to be assumed (via STS) by external account
    * IAM policy: allow ```lambda:InvokeFunction``` for exposed API Lambda functions in [Lambda API contract](../architecture/api-contracts/isolation-payment-gateway.md)
    * Trust policy: allow ```sts:AssumeRole``` to IAM role (principal) in external account
        * The principal must not only be a valid ARN but also an ARN referencing an existing IAM role - no dummy placeholder allowed
* IAM role in external account can be a Lambda Execution role
* Both systems must agree on naming conventions (to decouple IAM Role & Policy creation with IaC)
    * In exposing AWS Account: ```arn:aws:iam::<account ID>:role/<target-environment>-isolation-payment-gateway```
    * In external AWS Account: ```arn:aws:iam::<account ID>:role/<target-environment>-ipc-token-verify-consume-lambda```
* One specific target environment in the external AWS account can invoke the Lambda functiuons in exaclty one target-environment in the exposing AWS account (1:1 mappping)

## Isolation Payment Claim Tokens

States (linear flow - only current state must pe persisted):
- (not existing)
- ```created```  : New ```ipcToken``` created (generated from secure random source, 32 bytes, hex representation). Token is invalid.
- ```valid```    : Additional data fields associated with the token (stored in the token item in DynamoDB). Token is valid and can be consumed (once).
- ```consumed``` : Token has been consumed (i.e. isolation payment claim has been sent by the Isolation Payment Gateway to CTAS). Token is invalid.
- (deleted)      : Token is automatically deleted (DynamoDB TTL) ```8 weeks``` after creation (```expirationTimestamp```)

Timestamp of state transitions are stored in invidivual columns of the token item in DynamoDB:
- createdTimestamp: Timestamp of the ```/isolation-payment/ipc-token/create``` call
- updatedTimestamp: Timestamp of the ```/isolation-payment/ipc-token/update``` call
- validatedTimestamp: Timestamp of the last ```<target-environment>-ipc-token-verify``` invocation
- consumedTimestamp: Timestamp of the ```<target-environment>-ipc-token-consume``` invocation
- All timestamps are stored in Unix epoch time format in seconds (same format as the TTL column)

State transitions must be audit logged:
- CloudWatch Logs
- Information: ```ipcToken``` and triggering action, but no user data like isolation start date and end date

## References

- https://aws.amazon.com/de/premiumsupport/knowledge-center/lambda-function-assume-iam-role/
- https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/time-to-live-ttl-before-you-start.html
