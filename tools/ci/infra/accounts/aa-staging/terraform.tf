terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 3.0"
    }
  }

  backend "s3" {
    bucket = "tf-state-0746-3426-4982-rand-7427027392"
    key    = "ci-dev-account"
    region = "eu-west-2"
  }
}

provider "aws" {
  region = "eu-west-2"
}
