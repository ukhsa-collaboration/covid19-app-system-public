locals {
  name_prefix = "${var.name}-${terraform.workspace}"
  fqdn        = "${local.name_prefix}.${var.domain}"
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
  comment         = "Upload APIs for ${terraform.workspace}"


  default_cache_behavior {
    target_origin_id       = var.risky-post-districts-upload-endpoint
    viewer_protocol_policy = "https-only"
    min_ttl                = 0
    max_ttl                = 0
    default_ttl            = 0
    allowed_methods        = ["HEAD", "DELETE", "POST", "GET", "OPTIONS", "PUT", "PATCH"]
    cached_methods         = ["HEAD", "GET", "OPTIONS"]

    forwarded_values {
      query_string = true

      cookies {
        forward = "all"
      }

      headers = ["User-Agent"]
    }
  }

  origin {
    domain_name = replace(var.risky-post-districts-upload-endpoint, "/^https?://([^/]*).*/", "$1")
    origin_id   = var.risky-post-districts-upload-endpoint

    custom_header {
      name  = "x-custom-oai"
      value = var.custom_oai
    }

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "https-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }
  ordered_cache_behavior {
    path_pattern     = var.risky-post-districts-upload-path
    allowed_methods  = ["HEAD", "DELETE", "POST", "GET", "OPTIONS", "PUT", "PATCH"]
    cached_methods   = ["HEAD", "GET", "OPTIONS"]
    target_origin_id = var.risky-post-districts-upload-endpoint

    default_ttl = 0
    min_ttl     = 0
    max_ttl     = 0

    forwarded_values {
      query_string = true
      cookies {
        forward = "all"
      }

      headers = ["User-Agent"]
    }

    viewer_protocol_policy = "https-only"
  }
  ordered_cache_behavior {
    path_pattern     = var.risky-post-districts-upload-health-path
    allowed_methods  = ["HEAD", "DELETE", "POST", "GET", "OPTIONS", "PUT", "PATCH"]
    cached_methods   = ["HEAD", "GET", "OPTIONS"]
    target_origin_id = var.risky-post-districts-upload-endpoint

    default_ttl = 0
    min_ttl     = 0
    max_ttl     = 0

    forwarded_values {
      query_string = true
      cookies {
        forward = "all"
      }

      headers = ["User-Agent"]
    }

    viewer_protocol_policy = "https-only"
  }

  origin {
    domain_name = replace(var.risky-venues-upload-endpoint, "/^https?://([^/]*).*/", "$1")
    origin_id   = var.risky-venues-upload-endpoint

    custom_header {
      name  = "x-custom-oai"
      value = var.custom_oai
    }

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "https-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }
  ordered_cache_behavior {
    path_pattern     = var.risky-venues-upload-path
    allowed_methods  = ["HEAD", "DELETE", "POST", "GET", "OPTIONS", "PUT", "PATCH"]
    cached_methods   = ["HEAD", "GET", "OPTIONS"]
    target_origin_id = var.risky-venues-upload-endpoint

    default_ttl = 0
    min_ttl     = 0
    max_ttl     = 0

    forwarded_values {
      query_string = true
      cookies {
        forward = "all"
      }

      headers = ["User-Agent"]
    }

    viewer_protocol_policy = "https-only"
  }
  ordered_cache_behavior {
    path_pattern     = var.risky-venues-upload-health-path
    allowed_methods  = ["HEAD", "DELETE", "POST", "GET", "OPTIONS", "PUT", "PATCH"]
    cached_methods   = ["HEAD", "GET", "OPTIONS"]
    target_origin_id = var.risky-venues-upload-endpoint

    default_ttl = 0
    min_ttl     = 0
    max_ttl     = 0

    forwarded_values {
      query_string = true
      cookies {
        forward = "all"
      }

      headers = ["User-Agent"]
    }

    viewer_protocol_policy = "https-only"
  }

  origin {
    domain_name = replace(var.test-results-upload-endpoint, "/^https?://([^/]*).*/", "$1")
    origin_id   = var.test-results-upload-endpoint

    custom_header {
      name  = "x-custom-oai"
      value = var.custom_oai
    }

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "https-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }
  ordered_cache_behavior {
    path_pattern     = var.test-results-upload-path
    allowed_methods  = ["HEAD", "DELETE", "POST", "GET", "OPTIONS", "PUT", "PATCH"]
    cached_methods   = ["HEAD", "GET", "OPTIONS"]
    target_origin_id = var.test-results-upload-endpoint

    default_ttl = 0
    min_ttl     = 0
    max_ttl     = 0

    forwarded_values {
      query_string = true
      cookies {
        forward = "all"
      }

      headers = ["User-Agent"]
    }

    viewer_protocol_policy = "https-only"
  }
  origin {
    domain_name = replace(var.isolation_payment_endpoint, "/^https?://([^/]*).*/", "$1")
    origin_id   = var.isolation_payment_endpoint

    custom_header {
      name  = "x-custom-oai"
      value = var.custom_oai
    }

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "https-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }
  ordered_cache_behavior {
    path_pattern     = var.isolation_payment_path
    allowed_methods  = ["HEAD", "DELETE", "POST", "GET", "OPTIONS", "PUT", "PATCH"]
    cached_methods   = ["HEAD", "GET", "OPTIONS"]
    target_origin_id = var.isolation_payment_endpoint

    default_ttl = 0
    min_ttl     = 0
    max_ttl     = 0

    forwarded_values {
      query_string = true
      cookies {
        forward = "all"
      }

      headers = ["User-Agent"]
    }

    viewer_protocol_policy = "https-only"
  }
  ordered_cache_behavior {
    path_pattern     = var.isolation_payment_health_path
    allowed_methods  = ["HEAD", "DELETE", "POST", "GET", "OPTIONS", "PUT", "PATCH"]
    cached_methods   = ["HEAD", "GET", "OPTIONS"]
    target_origin_id = var.isolation_payment_endpoint

    default_ttl = 0
    min_ttl     = 0
    max_ttl     = 0

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

resource "aws_shield_protection" "this" {
  count        = var.enable_shield_protection == true ? 1 : 0
  name         = "${local.name_prefix}-cf-shield"
  resource_arn = aws_cloudfront_distribution.this.arn
}
