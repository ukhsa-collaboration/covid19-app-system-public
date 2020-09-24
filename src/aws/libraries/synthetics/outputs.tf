output "function_name" {
  // if var.enabled is false, then the synthetics_canary.watchdog list will be empty
  value = length(synthetics_canary.watchdog) > 0 ? synthetics_canary.watchdog[0].name : local.function_name
}
