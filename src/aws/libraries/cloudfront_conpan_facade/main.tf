locals {
  fqdn            = "${var.name}-${terraform.workspace}.${var.domain}"
  allowed_methods = ["HEAD", "DELETE", "POST", "GET", "OPTIONS", "PUT", "PATCH"]
  cached_methods  = ["HEAD", "GET", "OPTIONS"]
}

provider "aws" {
  alias  = "useast"
  region = "us-east-1"
}

data "aws_acm_certificate" "this" {
  domain      = "*.${var.domain}"
  provider    = aws.useast
  types       = ["AMAZON_ISSUED"]
  most_recent = true
}

resource "aws_cloudfront_distribution" "this" {
  enabled         = true
  is_ipv6_enabled = true
  aliases         = [local.fqdn]
  price_class     = "PriceClass_100"
  web_acl_id      = var.web_acl_arn
  comment         = "Control Panel website for ${terraform.workspace}"

  tags = {
    Environment = terraform.workspace
    Service     = var.name
  }

  default_root_object = "index.html"

  origin {
    domain_name = var.bucket_regional_domain_name
    origin_id   = var.bucket_regional_domain_name

    s3_origin_config {
      origin_access_identity = var.origin_access_identity_path
    }
  }

  default_cache_behavior {
    allowed_methods = local.allowed_methods
    cached_methods  = local.cached_methods

    target_origin_id = var.bucket_regional_domain_name

    min_ttl     = 0
    max_ttl     = 0
    default_ttl = 0

    forwarded_values {
      query_string = false
      cookies {
        forward = "none"
      }
      headers = ["Origin"]
    }

    viewer_protocol_policy = "allow-all"
  }

  ordered_cache_behavior {
    allowed_methods = local.allowed_methods
    cached_methods  = local.cached_methods

    target_origin_id = var.bucket_regional_domain_name
    path_pattern     = var.conpan_path

    default_ttl = 0
    min_ttl     = 0
    max_ttl     = 0

    forwarded_values {
      query_string = false
      cookies {
        forward = "none"
      }
      headers = ["Origin"]
    }

    viewer_protocol_policy = "redirect-to-https"
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    acm_certificate_arn            = data.aws_acm_certificate.this.arn
    cloudfront_default_certificate = false
    ssl_support_method             = "sni-only"
    minimum_protocol_version       = "TLSv1.2_2018"
  }
}

data "aws_route53_zone" "selected" {
  name         = var.domain
  private_zone = false
}

resource "aws_route53_record" "this" {
  zone_id = data.aws_route53_zone.selected.zone_id
  name    = local.fqdn
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.this.domain_name
    zone_id                = aws_cloudfront_distribution.this.hosted_zone_id
    evaluate_target_health = false
  }
}
