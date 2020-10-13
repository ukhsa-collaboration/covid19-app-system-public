locals {
  us_east_1 = "us-east-1"
}

provider "aws" {
  alias  = "useast"
  region = local.us_east_1
}