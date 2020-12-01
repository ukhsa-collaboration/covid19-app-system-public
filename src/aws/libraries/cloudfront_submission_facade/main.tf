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
  comment         = "Submission APIs for ${terraform.workspace}"

  origin {
    domain_name = replace(var.diagnosis_keys_submission_endpoint, "/^https?://([^/]*).*/", "$1")
    origin_id   = var.diagnosis_keys_submission_endpoint

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
    path_pattern     = var.diagnosis_keys_submission_path
    allowed_methods  = ["HEAD", "DELETE", "POST", "GET", "OPTIONS", "PUT", "PATCH"]
    cached_methods   = ["HEAD", "GET", "OPTIONS"]
    target_origin_id = var.diagnosis_keys_submission_endpoint

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
    path_pattern     = var.diagnosis_keys_submission_health_path
    allowed_methods  = ["HEAD", "DELETE", "POST", "GET", "OPTIONS", "PUT", "PATCH"]
    cached_methods   = ["HEAD", "GET", "OPTIONS"]
    target_origin_id = var.diagnosis_keys_submission_endpoint

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
    domain_name = replace(var.analytics_submission_endpoint, "/^https?://([^/]*).*/", "$1")
    origin_id   = var.analytics_submission_endpoint

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
    path_pattern     = var.analytics_submission_path
    allowed_methods  = ["HEAD", "DELETE", "POST", "GET", "OPTIONS", "PUT", "PATCH"]
    cached_methods   = ["HEAD", "GET", "OPTIONS"]
    target_origin_id = var.analytics_submission_endpoint

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
    path_pattern     = var.analytics_submission_health_path
    allowed_methods  = ["HEAD", "DELETE", "POST", "GET", "OPTIONS", "PUT", "PATCH"]
    cached_methods   = ["HEAD", "GET", "OPTIONS"]
    target_origin_id = var.analytics_submission_endpoint

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
    domain_name = replace(var.analytics_events_submission_endpoint, "/^https?://([^/]*).*/", "$1")
    origin_id   = var.analytics_events_submission_endpoint

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
    path_pattern     = var.analytics_events_submission_path
    allowed_methods  = ["HEAD", "DELETE", "POST", "GET", "OPTIONS", "PUT", "PATCH"]
    cached_methods   = ["HEAD", "GET", "OPTIONS"]
    target_origin_id = var.analytics_events_submission_endpoint

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

  default_cache_behavior {
    target_origin_id       = var.diagnosis_keys_submission_endpoint
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

      headers = ["*"]
    }
  }

  origin {
    domain_name = replace(var.exposure_notification_circuit_breaker_endpoint, "/^https?://([^/]*).*/", "$1")
    origin_id   = var.exposure_notification_circuit_breaker_endpoint

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
    path_pattern     = var.exposure_notification_circuit_breaker_path
    allowed_methods  = ["HEAD", "DELETE", "POST", "GET", "OPTIONS", "PUT", "PATCH"]
    cached_methods   = ["HEAD", "GET", "OPTIONS"]
    target_origin_id = var.exposure_notification_circuit_breaker_endpoint

    default_ttl = 0
    min_ttl     = 0
    max_ttl     = 0

    forwarded_values {
      query_string = true
      cookies {
        forward = "all"
      }

      headers = ["Authorization", "User-Agent"] # forward this header for GET requests as well
    }

    viewer_protocol_policy = "https-only"
  }

  origin {
    domain_name = replace(var.risky_venues_circuit_breaker_endpoint, "/^https?://([^/]*).*/", "$1")
    origin_id   = var.risky_venues_circuit_breaker_endpoint

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
    path_pattern     = var.risky_venues_circuit_breaker_path
    allowed_methods  = ["HEAD", "DELETE", "POST", "GET", "OPTIONS", "PUT", "PATCH"]
    cached_methods   = ["HEAD", "GET", "OPTIONS"]
    target_origin_id = var.risky_venues_circuit_breaker_endpoint

    default_ttl = 0
    min_ttl     = 0
    max_ttl     = 0

    forwarded_values {
      query_string = true
      cookies {
        forward = "all"
      }

      headers = ["Authorization", "User-Agent"] # forward this header for GET requests as well
    }

    viewer_protocol_policy = "https-only"
  }

  origin {
    domain_name = replace(var.virology_kit_endpoint, "/^https?://([^/]*).*/", "$1")
    origin_id   = var.virology_kit_endpoint

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
    path_pattern     = var.virology_kit_path
    allowed_methods  = ["HEAD", "DELETE", "POST", "GET", "OPTIONS", "PUT", "PATCH"]
    cached_methods   = ["HEAD", "GET", "OPTIONS"]
    target_origin_id = var.virology_kit_endpoint

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

  origin {
    domain_name = replace(var.empty_submission_endpoint, "/^https?://([^/]*).*/", "$1")
    origin_id   = var.empty_submission_endpoint

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
    path_pattern     = var.empty_submission_path
    allowed_methods  = ["HEAD", "DELETE", "POST", "GET", "OPTIONS", "PUT", "PATCH"]
    cached_methods   = ["HEAD", "GET", "OPTIONS"]
    target_origin_id = var.empty_submission_endpoint

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

  #retain_on_delete = # not a good idead: "true" causes "Error: CloudFrontOriginAccessIdentityInUse: The CloudFront origin access identity is still being used" in other places

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
