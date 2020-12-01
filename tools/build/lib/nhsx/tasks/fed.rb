namespace :fed do
  NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
        desc "enable federation connector for given environment"
        task "connector:enable:#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::Terraform
          include NHSx::AWS
          terraform_configuration = File.join($configuration.base, "src/aws/accounts", account)
          env_identifier =  target_environment_name(tgt_env, account, $configuration)
          environment_vars = {"DOWNLOAD_ENABLED" => "true", "UPLOAD_ENABLED" => "true"}
          function_name = "#{env_identifier}-federation-key-proc"
          refresh_workspace(terraform_configuration, $configuration)
          update_lambda_env_vars(function_name, environment_vars, $configuration)
          refresh_workspace(terraform_configuration, $configuration)
        end
        desc "disable federation connector for given environment"
        task "connector:disable:#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::Terraform
          include NHSx::AWS
          terraform_configuration = File.join($configuration.base, "src/aws/accounts", account)
          env_identifier =  target_environment_name(tgt_env, account, $configuration)
          environment_vars = {"DOWNLOAD_ENABLED" => "false", "UPLOAD_ENABLED" => "false"}
          function_name = "#{env_identifier}-federation-key-proc"
          refresh_workspace(terraform_configuration, $configuration)
          update_lambda_env_vars(function_name, environment_vars, $configuration)
          refresh_workspace(terraform_configuration, $configuration)
        end
      end
   end
end

