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
  comment         = "Distribution APIs for ${terraform.workspace}"

  default_cache_behavior {
    target_origin_id       = var.exposure_configuration_bucket_regional_domain_name
    viewer_protocol_policy = "https-only"
    min_ttl                = var.distribution_cache_ttl
    max_ttl                = var.distribution_cache_ttl
    default_ttl            = var.distribution_cache_ttl
    compress               = true
    allowed_methods = [
      "DELETE",
      "GET",
      "HEAD",
      "OPTIONS",
      "PATCH",
      "POST",
    "PUT"]
    cached_methods = [
      "GET",
    "HEAD"]

    forwarded_values {
      query_string = false

      cookies {
        forward = "none"
      }
    }
  }
  #retain_on_delete = # not a good idead: "true" causes "Error: CloudFrontOriginAccessIdentityInUse: The CloudFront origin access identity is still being used" in other places
  origin {
    domain_name = var.exposure_configuration_bucket_regional_domain_name
    origin_id   = var.exposure_configuration_bucket_regional_domain_name

    s3_origin_config {
      origin_access_identity = var.exposure_configuration_origin_access_identity_path
    }
  }
  ordered_cache_behavior {
    path_pattern     = "/${var.name}/${var.exposure_configuration_payload}"
    allowed_methods  = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = var.exposure_configuration_bucket_regional_domain_name
    compress         = true
    min_ttl          = var.distribution_cache_ttl
    max_ttl          = var.distribution_cache_ttl
    default_ttl      = var.distribution_cache_ttl

    forwarded_values {
      query_string = true
      cookies {
        forward = "all"
      }
    }

    viewer_protocol_policy = "https-only"
  }

  origin {
    domain_name = var.risky_post_district_distribution_bucket_regional_domain_name
    origin_id   = var.risky_post_district_distribution_bucket_regional_domain_name

    s3_origin_config {
      origin_access_identity = var.risky_post_district_origin_access_identity_path
    }
  }
  ordered_cache_behavior {
    path_pattern     = "/${var.name}/${var.risky_post_district_payload}"
    allowed_methods  = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = var.risky_post_district_distribution_bucket_regional_domain_name
    compress         = true
    min_ttl          = var.distribution_cache_ttl
    max_ttl          = var.distribution_cache_ttl
    default_ttl      = var.distribution_cache_ttl

    forwarded_values {
      query_string = true
      cookies {
        forward = "all"
      }
    }

    viewer_protocol_policy = "https-only"
  }

  origin {
    domain_name = var.risky_post_district_distribution_bucket_regional_domain_name
    origin_id   = var.risky_post_district_distribution_bucket_regional_domain_name

    s3_origin_config {
      origin_access_identity = var.risky_post_district_origin_access_identity_path
    }
  }
  ordered_cache_behavior {
    path_pattern     = "/${var.name}/${var.risky_post_district_v2_payload}"
    allowed_methods  = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = var.risky_post_district_distribution_bucket_regional_domain_name
    compress         = true
    min_ttl          = var.distribution_cache_ttl
    max_ttl          = var.distribution_cache_ttl
    default_ttl      = var.distribution_cache_ttl

    forwarded_values {
      query_string = true
      cookies {
        forward = "all"
      }
    }

    viewer_protocol_policy = "https-only"
  }

  origin {
    domain_name = var.risky_venues_bucket_regional_domain_name
    origin_id   = var.risky_venues_bucket_regional_domain_name

    s3_origin_config {
      origin_access_identity = var.risky_venues_origin_access_identity_path
    }
  }
  ordered_cache_behavior {
    path_pattern     = "/${var.name}/${var.risky_venues_payload}"
    allowed_methods  = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = var.risky_venues_bucket_regional_domain_name
    compress         = true
    min_ttl          = var.distribution_cache_ttl
    max_ttl          = var.distribution_cache_ttl
    default_ttl      = var.distribution_cache_ttl

    forwarded_values {
      query_string = true
      cookies {
        forward = "all"
      }
    }

    viewer_protocol_policy = "https-only"
  }

  origin {
    domain_name = var.self_isolation_bucket_regional_domain_name
    origin_id   = var.self_isolation_bucket_regional_domain_name

    s3_origin_config {
      origin_access_identity = var.self_isolation_origin_access_identity_path
    }
  }
  ordered_cache_behavior {
    path_pattern     = "/${var.name}/${var.self_isolation_payload}"
    allowed_methods  = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = var.self_isolation_bucket_regional_domain_name
    compress         = true
    min_ttl          = var.distribution_cache_ttl
    max_ttl          = var.distribution_cache_ttl
    default_ttl      = var.distribution_cache_ttl

    forwarded_values {
      query_string = true
      cookies {
        forward = "all"
      }
    }

    viewer_protocol_policy = "https-only"
  }

  origin {
    domain_name = var.symptomatic_questionnaire_bucket_regional_domain_name
    origin_id   = var.symptomatic_questionnaire_bucket_regional_domain_name

    s3_origin_config {
      origin_access_identity = var.symptomatic_questionnaire_origin_access_identity_path
    }
  }
  ordered_cache_behavior {
    path_pattern     = "/${var.name}/${var.symptomatic_questionnaire_payload}"
    allowed_methods  = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = var.symptomatic_questionnaire_bucket_regional_domain_name
    compress         = true
    min_ttl          = var.distribution_cache_ttl
    max_ttl          = var.distribution_cache_ttl
    default_ttl      = var.distribution_cache_ttl

    forwarded_values {
      query_string = true
      cookies {
        forward = "all"
      }
    }

    viewer_protocol_policy = "https-only"
  }

  origin {
    domain_name = var.diagnosis_keys_bucket_regional_domain_name
    origin_id   = var.diagnosis_keys_bucket_regional_domain_name

    s3_origin_config {
      origin_access_identity = var.diagnosis_keys_origin_access_identity_path
    }
  }
  ordered_cache_behavior {
    path_pattern     = var.diagnosis_keys_path_2hourly
    allowed_methods  = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = var.diagnosis_keys_bucket_regional_domain_name
    compress         = true
    min_ttl          = var.distribution_cache_ttl
    max_ttl          = var.distribution_cache_ttl
    default_ttl      = var.distribution_cache_ttl

    forwarded_values {
      query_string = true
      cookies {
        forward = "all"
      }
    }

    viewer_protocol_policy = "https-only"
  }
  ordered_cache_behavior {
    path_pattern     = var.diagnosis_keys_path_daily
    allowed_methods  = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = var.diagnosis_keys_bucket_regional_domain_name
    compress         = true
    min_ttl          = var.distribution_cache_ttl
    max_ttl          = var.distribution_cache_ttl
    default_ttl      = var.distribution_cache_ttl

    forwarded_values {
      query_string = true
      cookies {
        forward = "all"
      }
    }

    viewer_protocol_policy = "https-only"
  }

  origin {
    domain_name = var.availability_android_bucket_regional_domain_name
    origin_id   = var.availability_android_bucket_regional_domain_name

    s3_origin_config {
      origin_access_identity = var.availability_android_origin_access_identity_path
    }
  }
  ordered_cache_behavior {
    path_pattern     = "/${var.name}/${var.availability_android_payload}"
    allowed_methods  = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = var.availability_android_bucket_regional_domain_name
    compress         = true
    min_ttl          = var.distribution_cache_ttl
    max_ttl          = var.distribution_cache_ttl
    default_ttl      = var.distribution_cache_ttl

    forwarded_values {
      query_string = true
      cookies {
        forward = "all"
      }
    }

    viewer_protocol_policy = "https-only"
  }

  origin {
    domain_name = var.availability_ios_bucket_regional_domain_name
    origin_id   = var.availability_ios_bucket_regional_domain_name

    s3_origin_config {
      origin_access_identity = var.availability_ios_origin_access_identity_path
    }
  }
  ordered_cache_behavior {
    path_pattern     = "/${var.name}/${var.availability_ios_payload}"
    allowed_methods  = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = var.availability_ios_bucket_regional_domain_name
    compress         = true
    min_ttl          = var.distribution_cache_ttl
    max_ttl          = var.distribution_cache_ttl
    default_ttl      = var.distribution_cache_ttl

    forwarded_values {
      query_string = true
      cookies {
        forward = "all"
      }
    }

    viewer_protocol_policy = "https-only"
  }

  origin {
    domain_name = var.risky_venue_configuration_bucket_regional_domain_name
    origin_id   = var.risky_venue_configuration_bucket_regional_domain_name

    s3_origin_config {
      origin_access_identity = var.risky_venue_configuration_origin_access_identity_path
    }
  }
  ordered_cache_behavior {
    path_pattern     = "/${var.name}/${var.risky_venue_configuration_payload}"
    allowed_methods  = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = var.risky_venue_configuration_bucket_regional_domain_name
    compress         = true
    min_ttl          = var.distribution_cache_ttl
    max_ttl          = var.distribution_cache_ttl
    default_ttl      = var.distribution_cache_ttl

    forwarded_values {
      query_string = true
      cookies {
        forward = "all"
      }
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
