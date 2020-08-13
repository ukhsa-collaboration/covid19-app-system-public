locals {
  region = "eu-west-1" # where the synthetic canaries will be deployed - should differ from endpoints under test
}

terraform {
  backend "s3" {
    bucket = "tf-state-3988-5065-2026-rand-59382228347"
    key    = "agapi-syn-dev"
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
