namespace :maintenance do
  namespace :virology do
    NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        prerequisites = [:"login:#{account}"]
        desc "Deactivate virology api on #{tgt_env} target environment"
        task :"on:#{tgt_env}" => prerequisites do
          include NHSx::Maintenance
          deactivate_virology(tgt_env, account, $configuration)
        end

        desc "Activate virology api on #{tgt_env} target environment"
        task :"off:#{tgt_env}" => prerequisites do
          include NHSx::Maintenance
          activate_virology(tgt_env, account, $configuration)
        end
      end
    end
  end
  namespace :local_stats do
    NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        prerequisites = [:"login:#{account}"]
        desc "Deactivate local stats api on #{tgt_env} target environment"
        task :"on:#{tgt_env}" => prerequisites do
          include NHSx::Maintenance
          target_environment_config = target_environment_configuration(tgt_env, account, $configuration)
          disable_api(target_environment_config["local_stats_processing_function"], $configuration)
        end

        desc "Activate local stats api on #{tgt_env} target environment"
        task :"off:#{tgt_env}" => prerequisites do
          include NHSx::Maintenance
          target_environment_config = target_environment_configuration(tgt_env, account, $configuration)
          enable_api(target_environment_config["local_stats_processing_function"], $configuration)
        end
      end
    end
  end
end
