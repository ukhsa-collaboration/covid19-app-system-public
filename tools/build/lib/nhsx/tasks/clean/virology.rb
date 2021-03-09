namespace :clean do
  namespace :virology do
    NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        desc "Clean tables used for restore on #{tgt_env} target environment"
        task :"tables:#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::TargetEnvironment
          include NHSx::RestoreVirology
          target_environment = target_environment_name(tgt_env, account, $configuration)
          clean_restored_tables(target_environment, $configuration)
        end
      end
    end
  end
end
