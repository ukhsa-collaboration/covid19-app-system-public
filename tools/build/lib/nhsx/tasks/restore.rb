namespace :restore do
  namespace :virology do
    NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
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
      end
    end
  end
end

