# Synthetic Canaries
output "synthetic_probing_analytics_subm_endpoint" {
  value = module.agapi-syn.probe_analytics_function_name
}

output "synthetic_probing_diag_keys_subm_endpoint" {
  value = module.agapi-syn.probe_diag_keys_function_name
}

output "synthetic_probing_exp_notif_circ_brkr_endpoint" {
  value = module.agapi-syn.probe_exp_notif_circ_brkr_function_name
}

output "synthetic_probing_rsky_vnue_circ_brkr_endpoint" {
  value = module.agapi-syn.probe_rsky_vnue_circ_brkr_function_name
}

output "synthetic_probing_virology_test_subm_endpoint" {
  value = module.agapi-syn.probe_virology_test_function_name
}

output "synthetic_probing_risky_post_districts_upload_endpoint" {
  value = module.agapi-syn.probe_risky_post_districts_upload_function_name
}

output "synthetic_probing_risky_venues_upload_endpoint" {
  value = module.agapi-syn.probe_risky_venues_upload_function_name
}

output "synthetic_probing_virology_upload_endpoint" {
  value = module.agapi-syn.probe_virology_upload_function_name
}

output "synthetic_probing_availability_android_dstrb_endpoint" {
  value = module.agapi-syn.probe_availability_android_distribution_function_name
}

output "synthetic_probing_availability_ios_dstrb_endpoint" {
  value = module.agapi-syn.probe_availability_ios_distribution_function_name
}

output "synthetic_probing_diagnosis_keys_2hourly_dstrb_endpoint" {
  value = module.agapi-syn.probe_diagnosis_keys_2hourly_distribution_function_name
}

output "synthetic_probing_diagnosis_keys_daily_dstrb_endpoint" {
  value = module.agapi-syn.probe_diagnosis_keys_daily_distribution_function_name
}

output "synthetic_probing_exposure_configuration_dstrb_endpoint" {
  value = module.agapi-syn.probe_exposure_configuration_distribution_function_name
}

output "synthetic_probing_risky_post_district_dstrb_endpoint" {
  value = module.agapi-syn.probe_risky_post_district_distribution_function_name
}

output "synthetic_probing_risky_venues_dstrb_endpoint" {
  value = module.agapi-syn.probe_risky_venues_distribution_function_name
}

output "synthetic_probing_self_isolation_dstrb_endpoint" {
  value = module.agapi-syn.probe_self_isolation_distribution_function_name
}

output "synthetic_probing_symptomatic_questionnaire_dstrb_endpoint" {
  value = module.agapi-syn.probe_symptomatic_questionnaire_distribution_function_name
}
