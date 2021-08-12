output "pr_webhook_app_system" {
  value = module.app-system-ci.pr_webhook_app_system
}

output "ci_webhook_app_system" {
  value = module.app-system-ci.ci_webhook_app_system
}

output "devenv_webhook_app_system" {
  value = module.app-system-ci.devenv_webhook_app_system
}
