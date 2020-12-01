#!/usr/bin/env bash

alias ro-login-prod="aws-mfa --duration 3600 --profile nhs-auth --assume-role arn:aws:iam::123456789012:role/prod-ReadOnlyUser --long-term-suffix none --short-term-suffix prod && export AWS_PROFILE=nhs-auth-prod"
alias dp-login-prod="aws-mfa --duration 3600 --profile nhs-auth --assume-role arn:aws:iam::123456789012:role/prod-ApplicationDeploymentUser --long-term-suffix none --short-term-suffix prod && export AWS_PROFILE=nhs-auth-prod"
alias acc-login-prod="aws-mfa --duration 3600 --profile nhs-auth --assume-role arn:aws:iam::123456789012:role/prod-AccountDeploymentUser --long-term-suffix none --short-term-suffix prod && export AWS_PROFILE=nhs-auth-prod"

alias ro-login-staging="aws-mfa --duration 3600 --profile nhs-auth --assume-role arn:aws:iam::123456789012:role/staging-ReadOnlyUser --long-term-suffix none --short-term-suffix staging && export AWS_PROFILE=nhs-auth-staging"
alias dp-login-staging="aws-mfa --duration 3600 --profile nhs-auth --assume-role arn:aws:iam::123456789012:role/staging-ApplicationDeploymentUser --long-term-suffix none --short-term-suffix staging && export AWS_PROFILE=nhs-auth-staging"
alias acc-login-staging="aws-mfa --duration 3600 --profile nhs-auth --assume-role arn:aws:iam::123456789012:role/staging-AccountDeploymentUser --long-term-suffix none --short-term-suffix staging && export AWS_PROFILE=nhs-auth-staging"

echo 'defined aliases (ro|dp|acc)-login-(prod|staging)'
