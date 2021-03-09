namespace :aae do
  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      %w[json parquet].each do |format|
        desc "enable aae #{format} upload for #{tgt_env} environment"
        task "upload:#{format}:enable:#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::TargetEnvironment
          target_env_config = target_environment_configuration(tgt_env, account, $configuration)
          uuid = get_aae_mapping_uuid(target_env_config, format)
          enable_event_source_mapping(uuid, $configuration)
        end

        desc "disable aae #{format} upload for #{tgt_env} environment"
        task "upload:#{format}:disable:#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::TargetEnvironment
          target_env_config = target_environment_configuration(tgt_env, account, $configuration)
          uuid = get_aae_mapping_uuid(target_env_config, format)
          disable_event_source_mapping(uuid, $configuration)
        end

        desc "move one sqs event from aae dlq queue back to original queue for #{tgt_env} environment"
        task "move:#{format}:sqs:event:#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::SQS
          include NHSx::Terraform
          env_identifier = target_environment_name(tgt_env, account, $configuration)
          prefix = format == "json" ? "events-" : ""
          src = "#{env_identifier}-aae-mobile-analytics-#{prefix + format}-export-dlq"
          dst = "#{env_identifier}-aae-mobile-analytics-#{prefix + format}-export"
          move_and_delete_all_sqs_events(src, dst)
        end
      end
    end
  end
end
