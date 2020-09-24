terraform {
  backend "s3" {
    bucket = "tf-state-3988-5065-2026-rand-59382228347-w"
    key    = "analytics-dev-account"
    region = "eu-west-2"
  }
}

provider "aws" {
  region  = "eu-west-2"
  version = "~> 2.0"
}
