terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 3.0"
    }
  }

  backend "s3" {
    bucket = "tf-state-****-****-****-****-***********-w"
    key    = "mocks-dev-account"
    region = "eu-west-2"
  }
}

provider "aws" {
  region = "eu-west-2"
}
