module "cloudwatch_analytics" {
  count                       = local.aae_export_is_enabled ? 1 : 0
  source                      = "./modules/ops_monitoring"
  env                         = terraform.workspace
  advanced_analytics_function = local.aae_export_is_enabled ? module.advanced_analytics_export[0].lambda_function_name : null
  tags                        = var.tags
}

