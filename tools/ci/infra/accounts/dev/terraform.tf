terraform {
  backend "s3" {
    bucket = "tf-state-****-****-****-****-***********-w"
    key    = "ci-dev-account"
    region = "eu-west-2"
  }
}

provider "aws" {
  region  = "eu-west-2"
  version = "~> 2.0"
}
