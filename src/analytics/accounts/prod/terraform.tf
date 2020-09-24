terraform {
  backend "s3" {
    bucket = "tf-state-0276-8143-7063-rand-8746904054"
    key    = "analytics"
    region = "eu-west-2"
  }
}

provider "aws" {
  region  = "eu-west-2"
  version = "~> 2.0"
}
