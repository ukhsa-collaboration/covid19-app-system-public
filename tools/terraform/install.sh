#!/usr/bin/env bash

set -Eeuo pipefail

TERRAFORM_VERSION="$1"
TERRAFORM_URL=https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip
TERRAFORM_ARCHIVE=terraform_${TERRAFORM_VERSION}_linux_amd64.zip
CURRENT_TERRAFORM_VERSION=$(terraform version -json | jq '.terraform_version' | sed -e 's/^"//' -e 's/"$//')

if [[ $CURRENT_TERRAFORM_VERSION = $TERRAFORM_VERSION ]]; then
  echo "Terraform $TERRAFORM_VERSION is already installed"
  exit 0
fi

wget -q ${TERRAFORM_URL} && \
  unzip ${TERRAFORM_ARCHIVE} && \
  chmod +x terraform && \
  sudo mv terraform /usr/local/bin && \
  rm ${TERRAFORM_ARCHIVE}
