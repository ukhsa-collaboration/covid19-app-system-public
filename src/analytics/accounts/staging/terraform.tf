terraform {
  backend "s3" {
    bucket = "tf-state-7664-3813-6456-rand-0939845029"
    key    = "analytics"
    region = "eu-west-2"
  }
}

provider "aws" {
  region  = "eu-west-2"
  version = "~> 2.0"
}
