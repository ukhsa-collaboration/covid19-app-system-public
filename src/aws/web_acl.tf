data "aws_wafv2_web_acl" "this" {
  name     = var.waf2_web_acl
  scope    = "CLOUDFRONT"
  provider = aws.us_east
}

provider "aws" {
  alias  = "us_east"
  region = "us-east-1"
}
