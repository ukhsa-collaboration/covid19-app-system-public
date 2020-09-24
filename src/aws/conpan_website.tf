module "conpan_website" {
  source                   = "./modules/conpan_website"
  name                     = "conpan"
  base_domain              = var.base_domain
  logs_bucket_id           = var.logs_bucket_id
  aws_wafv2_web_acl_arn    = data.aws_wafv2_web_acl.this.arn
  force_destroy_s3_buckets = var.force_destroy_s3_buckets
  enable_shield_protection = var.enable_shield_protection
}

output "conpan_store" {
  value = module.conpan_website.store.bucket.id
}

output "conpan_endpoint" {
  value = "https://${module.conpan_website.distribution_domain_name}/"
}
