namespace :publish do
  namespace :data do
    NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS["dev"].each do |tgt_env|
      desc "Publish exposure-configuration test data to #{tgt_env}"
      task :"exposure_configuration:#{tgt_env}" do
        include NHSx::Publish
        include NHSx::Terraform
        target_config = target_environment_configuration(tgt_env, "dev", $configuration)
        terraform_configuration = File.join($configuration.base, NHSx::TargetEnvironment::CTA_DEV_ACCOUNT)
        select_workspace(tgt_env, terraform_configuration, $configuration)
        static_content_file = $configuration.static_content
        publish_static_content(static_content_file, "distribution/exposure-configuration", "exposure_configuration_distribution_store", target_config, "", $configuration)
      end
      desc "Publish self-isolation test data to #{tgt_env}"
      task :"self_isolation:#{tgt_env}" do
        include NHSx::Publish
        include NHSx::Terraform
        target_config = target_environment_configuration(tgt_env, "dev", $configuration)
        terraform_configuration = File.join($configuration.base, NHSx::TargetEnvironment::CTA_DEV_ACCOUNT)
        select_workspace(tgt_env, terraform_configuration, $configuration)
        static_content_file = $configuration.static_content
        publish_static_content(static_content_file, "distribution/self-isolation", "self_isolation_distribution_store", target_config, "", $configuration)
      end
      desc "Publish analytics test data to #{tgt_env}"
      task :"analytics_data:#{tgt_env}" do
        include NHSx::Publish
        include NHSx::Terraform
        terraform_configuration = File.join($configuration.base, NHSx::TargetEnvironment::CTA_DEV_ACCOUNT)
        select_workspace(tgt_env, terraform_configuration, $configuration)
        test_data_folder = $configuration.test_data
        publish_test_files(test_data_folder, "analytics-data", "analytics_submission_store", tgt_env, $configuration)
      end
    end
  end
end
