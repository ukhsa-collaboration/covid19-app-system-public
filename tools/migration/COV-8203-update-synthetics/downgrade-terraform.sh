#!/usr/bin/env bash

export TERRAFORM_VERSION=0.12.29
export TERRAFORM_URL=https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip
export TERRAFORM_ARCHIVE=terraform_${TERRAFORM_VERSION}_linux_amd64.zip
cd /work
sudo wget -q ${TERRAFORM_URL} && sudo unzip ${TERRAFORM_ARCHIVE} && sudo cp terraform /usr/local/bin && sudo chmod +x /usr/local/bin/terraform
find src -type d -name .terraform -print0 | xargs -0 rm -rf