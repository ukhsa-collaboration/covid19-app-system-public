namespace :fed do
  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      desc "enable federation connector for given environment"
      task "connector:enable:#{tgt_env}" => [:"login:#{account}"] do
        include NHSx::Terraform
        include NHSx::AWS
        target_config = target_environment_configuration(tgt_env, account, $configuration)
        terraform_configuration = File.join($configuration.base, "src/aws/accounts", account)

        download_function_name = target_config["federation_keys_processing_download_function"]
        upload_function_name = target_config["federation_keys_processing_upload_function"]

        refresh_workspace(terraform_configuration, $configuration)
        update_lambda_env_vars(download_function_name, { "DOWNLOAD_ENABLED_WORKSPACES" => "*" }, $configuration)
        update_lambda_env_vars(upload_function_name, { "UPLOAD_ENABLED_WORKSPACES" => "*" }, $configuration)
        refresh_workspace(terraform_configuration, $configuration)
      end
      desc "disable federation connector for given environment"
      task "connector:disable:#{tgt_env}" => [:"login:#{account}"] do
        include NHSx::Terraform
        include NHSx::AWS
        target_config = target_environment_configuration(tgt_env, account, $configuration)
        terraform_configuration = File.join($configuration.base, "src/aws/accounts", account)

        download_function_name = target_config["federation_keys_processing_download_function"]
        upload_function_name = target_config["federation_keys_processing_upload_function"]

        refresh_workspace(terraform_configuration, $configuration)
        update_lambda_env_vars(download_function_name, { "DOWNLOAD_ENABLED_WORKSPACES" => "" }, $configuration)
        update_lambda_env_vars(upload_function_name, { "UPLOAD_ENABLED_WORKSPACES" => "" }, $configuration)
        refresh_workspace(terraform_configuration, $configuration)
      end
    end
  end
end
