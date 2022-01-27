#!/usr/bin/env bash

TERRAFORM_VERSION=$(terraform -v | grep 'You can update' | egrep -o '\b[0-9](\.[0-9]+)+\b')
GITVERSION_VERSION=$(curl -s 'https://github.com/GitTools/GitVersion/releases/latest' | grep 'releases/tag/' | egrep -o '\b[0-9](\.[0-9]+)+\b')
PYTHON_VERSION=$(curl -s https://www.python.org/downloads/ | grep -i 'Download Python ' | grep '/downloads/release/python' | egrep -o '\b[0-9](\.[0-9]+)+\b')
RUBY_VERSION=$(curl -s https://hub.docker.com/api/content/v1/products/images/ruby | jq '.full_description' | egrep -o '\b[0-9](\.[0-9]+)+-buster\b' | head -1)
FLAKE8_VERSION=$(curl -s https://pypi.org/project/flake8/ | egrep '^\s*flake8\s[.0-9]+\s*$' | egrep -o '\b[0-9](\.[0-9]+)+\b')

echo "Latest versions:"
echo "Terraform  ${TERRAFORM_VERSION}"
echo "GitVersion ${GITVERSION_VERSION}"
echo "Python     ${PYTHON_VERSION}"
echo "Ruby image ${RUBY_VERSION}"
echo "Flake8     ${FLAKE8_VERSION}"
