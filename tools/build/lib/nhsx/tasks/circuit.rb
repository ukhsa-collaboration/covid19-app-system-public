namespace :circuit do
  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      %w[exposure-notification venue-notification].each do |api|
        %w[initial poll].each do |parameter|
          ["YES", "NO", "PENDING"].each do |value|
            desc "set #{api} circuit breaker #{parameter} approval to \"#{value.downcase}\""
            task :"#{api}:#{parameter}:#{value.downcase}:#{tgt_env}" => [:"login:#{account}"] do
              include NHSx::Terraform
              include NHSx::AWS
              env_identifier = target_environment_name(tgt_env, account, $configuration)
              terraform_configuration = File.join($configuration.base, "src/aws/accounts", account)
              param_name = "/app/#{env_identifier}/cb/#{api}-#{parameter}"
              refresh_workspace(terraform_configuration, $configuration)
              update_ssm_parameter(param_name, value, $configuration)
              refresh_workspace(terraform_configuration, $configuration)
            end
          end
        end
      end
    end
  end
end
