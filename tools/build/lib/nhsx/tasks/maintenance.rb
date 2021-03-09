namespace :maintenance do
  namespace :virology do
    NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        prerequisites = [:"login:#{account}"]
        desc "Deactivate virology API on #{tgt_env} target environment"
        task :"on:#{tgt_env}" => prerequisites do
          include NHSx::Maintenance
          deactivate_virology(tgt_env, account, $configuration)
        end

        desc "Activate virology API on #{tgt_env} target environment"
        task :"off:#{tgt_env}" => prerequisites do
          include NHSx::Maintenance
          activate_virology(tgt_env, account, $configuration)
        end
      end
    end
  end
end
