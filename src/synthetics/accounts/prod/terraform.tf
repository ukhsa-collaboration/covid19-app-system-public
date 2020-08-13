locals {
  region = "eu-west-1" # where the synthetic canaries will be deployed - should differ from endpoints under test
}

terraform {
  backend "s3" {
    bucket = "tf-state-0276-8143-7063-rand-8746904054"
    key    = "agapi-syn-prod"
    region = "eu-west-2"
  }
}

provider "aws" {
  region  = local.region
  version = "~> 2.0"
}

provider "synthetics" {
  # Remove this provider once the mainline AWS provider has absorbed pull request
  # https://github.com/terraform-providers/terraform-provider-aws/pull/13140
  version = "v0.1.1"
  region  = local.region
}
