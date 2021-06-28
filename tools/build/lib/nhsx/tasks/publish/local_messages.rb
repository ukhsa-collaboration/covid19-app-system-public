namespace :publish do
  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      desc "Publish local messages to #{tgt_env} with MESSAGE_MAPPING and MESSAGES_METADATA"
      task :"local_messages:#{tgt_env}" => [:"login:#{account}", :"gen:signatures:#{account}"] do
        include NHSx::Publish
        include NHSx::Terraform
        target_config = target_environment_configuration(tgt_env, account, $configuration)
        terraform_configuration = File.join($configuration.base, APP_SYSTEM_ACCOUNTS, account)
        select_workspace(tgt_env, terraform_configuration, $configuration)
        generated_metadata = File.read(File.join($configuration.out, "signatures/local-messages.json.generated"))

        publish_static_content("#{$configuration.out}/local-messages/local-messages.json", "distribution/local-messages", "local_messages_distribution_store", target_config, generated_metadata, $configuration)
        tag("te-#{tgt_env}-local-messages", "Local messages configuration deployed on #{tgt_env}", $configuration) if tgt_env != "branch"
      end
    end
  end
end
