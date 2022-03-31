module "cache_artifacts" {
  source = "./libraries/artifacts_s3"
  name   = "${var.account}-build-cache-artifacts"
  lifecycle_rules = [{
    id         = "expire-all-after-90-days"
    prefix     = null
    enabled    = true
    transition = []
    days       = 90
  }]
}
