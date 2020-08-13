namespace :publish do
  desc "Publish the devenv docker image to the registry"
  task :devenv => [:"build:devenv"] do
    include NHSx::Docker
    publish_devenv_image($configuration)
  end

  desc "Publish the document reporting tool docker image to the registry"
  task :doreto => [:"build:doreto"] do
    include NHSx::Docker
    publish_doreto_image($configuration)
  end

  desc "Publish the control panel docker image to the registry"
  task :conpan => [:"build:conpan"] do
    include NHSx::Docker
    publish_conpan_image($configuration)
  end

  namespace :data do
    NHSx::TargetEnvironment::TARGET_ENVIRONMENTS["dev"].each do |tgt_env|
      desc "Publish risky-post-districts test data to #{tgt_env}"
      task :"post_districts:#{tgt_env}" do
        include NHSx::Publish
        include NHSx::Terraform
        terraform_configuration = File.join($configuration.base, NHSx::Terraform::DEV_ACCOUNT)
        select_workspace(tgt_env, terraform_configuration, $configuration)
        test_data_file = $configuration.test_data
        publish_test_data(test_data_file, "risky-post-districts", "post_districts_distribution_store", tgt_env, $configuration)
      end
      desc "Publish symptomatic-questionnaire test data to #{tgt_env}"
      task :"symptomatic_questionnaire:#{tgt_env}" do
        include NHSx::Publish
        include NHSx::Terraform
        terraform_configuration = File.join($configuration.base, NHSx::Terraform::DEV_ACCOUNT)
        select_workspace(tgt_env, terraform_configuration, $configuration)
        test_data_file = $configuration.test_data
        publish_test_data(test_data_file, "symptomatic-questionnaire", "symptomatic_questionnaire_distribution_store", tgt_env, $configuration)
      end
      desc "Publish risky-venues test data to #{tgt_env}"
      task :"risky_venues:#{tgt_env}" do
        include NHSx::Publish
        include NHSx::Terraform
        terraform_configuration = File.join($configuration.base, NHSx::Terraform::DEV_ACCOUNT)
        select_workspace(tgt_env, terraform_configuration, $configuration)
        test_data_file = $configuration.test_data
        publish_test_data(test_data_file, "risky-venues", "risky_venues_distribution_store", tgt_env, $configuration)
      end
      desc "Publish exposure-configuration test data to #{tgt_env}"
      task :"exposure_configuration:#{tgt_env}" do
        include NHSx::Publish
        include NHSx::Terraform
        terraform_configuration = File.join($configuration.base, NHSx::Terraform::DEV_ACCOUNT)
        select_workspace(tgt_env, terraform_configuration, $configuration)
        test_data_file = $configuration.test_data
        publish_test_data(test_data_file, "exposure-configuration", "exposure_configuration_distribution_store", tgt_env, $configuration)
      end
      desc "Publish self-isolation test data to #{tgt_env}"
      task :"self_isolation:#{tgt_env}" do
        include NHSx::Publish
        include NHSx::Terraform
        terraform_configuration = File.join($configuration.base, NHSx::Terraform::DEV_ACCOUNT)
        select_workspace(tgt_env, terraform_configuration, $configuration)
        test_data_file = $configuration.test_data
        publish_test_data(test_data_file, "self-isolation", "self_isolation_distribution_store", tgt_env, $configuration)
      end
      desc "Publish analytics test data to #{tgt_env}"
      task :"analytics_data:#{tgt_env}" do
        include NHSx::Publish
        include NHSx::Terraform
        terraform_configuration = File.join($configuration.base, NHSx::Terraform::DEV_ACCOUNT)
        select_workspace(tgt_env, terraform_configuration, $configuration)
        test_data_folder = $configuration.test_data
        publish_test_files(test_data_folder, "analytics-data", "analytics_submission_store", tgt_env, $configuration)
      end
    end
  end
end
