output "store" {
  value = module.conpan_website_s3
}

output "distribution_domain_name" {
  value = module.distribution_conpan.conpan_domain_name
}

output "distribution_arn" {
  value = module.distribution_conpan.distribution_arn
}
