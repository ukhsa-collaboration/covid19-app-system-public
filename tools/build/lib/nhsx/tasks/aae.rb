namespace :aae do
  NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      [true, false].each do |json_format|

        desc "enable aae #{json_format ? "json": "parquet"} upload for given environment"
        task "upload:#{json_format ? "json": "parquet"}:enable:#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::Terraform
          include NHSx::AWS
          terraform_configuration = File.join($configuration.base, NHSx::Terraform::APP_SYSTEM_ACCOUNTS, account)
          identifiers = aae_event_source_identifiers(tgt_env, terraform_configuration, $configuration, json_format)
          uuid = get_even_source_mapping_uuid(identifiers["function_name"], identifiers["event_source_arn"], identifiers["lambda_arn"], $configuration)
          raise GaudiError, "Could not find event source mapping uuid for #{identifiers["function_name"]}" if uuid.nil?
          enable_event_source_mapping(uuid, $configuration)
        end

        desc "disable aae #{json_format ? "json": "parquet"} upload for given environment"
        task "upload:#{json_format ? "json": "parquet"}:disable:#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::Terraform
          include NHSx::AWS
          terraform_configuration = File.join($configuration.base, NHSx::Terraform::APP_SYSTEM_ACCOUNTS, account)
          identifiers = aae_event_source_identifiers(tgt_env, terraform_configuration, $configuration, json_format)
          uuid = get_even_source_mapping_uuid(identifiers["function_name"], identifiers["event_source_arn"], identifiers["lambda_arn"], $configuration)
          raise GaudiError, "Could not find event source mapping uuid for #{identifiers["function_name"]}" if uuid.nil?
          disable_event_source_mapping(uuid, $configuration)
        end

      end
    end
  end
end