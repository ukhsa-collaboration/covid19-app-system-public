namespace :restore do
  namespace :virology do
    NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        prerequisites = [:"login:#{account}"]
        desc "Restore the virology data from RESTORE_AT on #{tgt_env} target environment"
        task :"#{tgt_env}" => prerequisites do
          include NHSx::Maintenance
          include NHSx::RestoreVirology

          deactivate_virology(tgt_env, account, $configuration)
          restore_virology(tgt_env, account, $configuration.restore_at, $configuration)
          activate_virology(tgt_env, account, $configuration)
        end
        desc "Clean tables used for restore on #{tgt_env} target environment"
        task :"clean:#{tgt_env}" do
          include NHSx::TargetEnvironment
          include NHSx::RestoreVirology
          target_environment = target_environment_name(tgt_env, account, $configuration)
          clean_restored_tables(target_environment, $configuration)
        end
      end
    end
  end
end
