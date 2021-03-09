namespace :create do
  namespace :ipc do
    NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        desc "Create and update an isolation payment token for England"
        task :"token:en:#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::API::Submission

          target_config = target_environment_configuration(tgt_env, account, $configuration)
          ipc_token = create_isolation_payment_token("England", target_config, $configuration)

          update_isolation_payment_token(ipc_token, target_config, $configuration)
          puts "Created and updated IPC token #{ipc_token}"
        end
        desc "Create and update an isolation payment token for Wales"
        task :"token:wa:#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::API::Submission

          target_config = target_environment_configuration(tgt_env, account, $configuration)
          ipc_token = create_isolation_payment_token("Wales", target_config, $configuration)

          update_isolation_payment_token(ipc_token, target_config, $configuration)
          puts "Created and updated IPC token #{ipc_token}"
        end
      end
    end
  end
end
