locals {
  fqdn = "${var.name}-${terraform.workspace}.${var.domain}"
}

provider "aws" {
  alias  = "useast"
  region = "us-east-1"
}

resource "aws_cloudfront_distribution" "this" {
  enabled         = true
  is_ipv6_enabled = true
  aliases         = [local.fqdn]
  price_class     = "PriceClass_100"
  web_acl_id      = var.web_acl_arn
  tags            = var.tags
  comment         = "Distribution APIs for ${terraform.workspace}"

  default_cache_behavior {
    target_origin_id       = var.risky_venues_messages_bucket_regional_domain_name
    viewer_protocol_policy = "https-only"
    min_ttl                = 0
    max_ttl                = 0
    default_ttl            = 0
    compress               = true
    allowed_methods = [
      "DELETE",
      "GET",
      "HEAD",
      "OPTIONS",
      "PATCH",
      "POST",
      "PUT"
    ]
    cached_methods = [
      "GET",
      "HEAD"
    ]

    forwarded_values {
      query_string = false

      cookies {
        forward = "none"
      }

      headers = ["User-Agent"]
    }
  }

  origin {
    domain_name = var.risky_venues_messages_bucket_regional_domain_name
    origin_id   = var.risky_venues_messages_bucket_regional_domain_name

    s3_origin_config {
      origin_access_identity = var.risky_venues_messages_origin_access_identity_path
    }
  }
  ordered_cache_behavior {
    path_pattern     = "/${var.name}/${var.risky_venues_messages_payload}"
    allowed_methods  = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = var.risky_venues_messages_bucket_regional_domain_name
    compress         = true
    default_ttl      = 0
    min_ttl          = 0
    max_ttl          = 0

    forwarded_values {
      query_string = true
      cookies {
        forward = "all"
      }

      headers = ["User-Agent"]
    }

    viewer_protocol_policy = "https-only"
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    acm_certificate_arn            = data.aws_acm_certificate.selected.arn
    cloudfront_default_certificate = false
    ssl_support_method             = "sni-only"
    minimum_protocol_version       = "TLSv1.2_2018"
  }
}

data "aws_acm_certificate" "selected" {
  domain      = "*.${var.domain}"
  provider    = aws.useast
  types       = ["AMAZON_ISSUED"]
  most_recent = true
  tags        = var.tags
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
