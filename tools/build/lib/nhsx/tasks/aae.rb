namespace :aae do
  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      %w[json parquet].each do |format|
        desc "enable AAE #{format} upload for #{tgt_env} environment"
        task "upload:#{format}:enable:#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::TargetEnvironment
          target_env_config = target_environment_configuration(tgt_env, account, $configuration)
          uuid = get_aae_mapping_uuid(target_env_config, format)
          enable_event_source_mapping(uuid, $configuration)
        end

        desc "disable AAE #{format} upload for #{tgt_env} environment"
        task "upload:#{format}:disable:#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::TargetEnvironment
          target_env_config = target_environment_configuration(tgt_env, account, $configuration)
          uuid = get_aae_mapping_uuid(target_env_config, format)
          disable_event_source_mapping(uuid, $configuration)
        end

        desc "move all sqs events from AAE dead-letter queue back to original queue for #{tgt_env} environment"
        task "move:#{format}:sqs:event:#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::SQS
          include NHSx::Terraform
          env_identifier = target_environment_name(tgt_env, account, $configuration)
          prefix = format == 'json' ? 'events-' : ''
          src = "#{env_identifier}-aae-mobile-analytics-#{prefix + format}-export-dlq"
          dst = "#{env_identifier}-aae-mobile-analytics-#{prefix + format}-export"
          move_and_delete_all_sqs_events(src, dst)
        end
      end
    end
  end

  NHSx::TargetEnvironment::AAE_TARGET_ACCOUNTS.each do |account, aae_account|
    desc "Store new entries with certificates for mTLS against the AAE #{aae_account} account in Secrets Manager on #{account} account"
    task :"upload:cert:store:#{account}" do
      include NHSx::Secret
      include NHSx::Certificate
      aae_config = aae_configuration($configuration, account)

      if account == 'staging'
        Rake::Task['login:dev'].invoke
        download_aae_secrets(aae_config, $configuration)
      end

      Rake::Task["login:#{account}"].invoke
      store_aae_certificates(aae_config, $configuration)
    end

    desc 'Clean up local certificates created during the create operation'
    task :"upload:cert:cleanup:#{account}" do
      include NHSx::Secret
      include NHSx::Certificate
      aae_config = aae_configuration($configuration, account)
      clean_aae_certificate(aae_config)
    end

    next unless account != 'staging'

    desc "Create new entries with certificates for mTLS against the AAE #{aae_account} account"
    task :"upload:cert:create:#{account}" do
      include NHSx::Secret
      include NHSx::Certificate
      aae_config = aae_configuration($configuration, account)
      create_aae_certificates(aae_config, $configuration)
    end

    desc "Update the stored APIM subscription key for the AAE #{aae_account} account in Secrets Manager on #{account} account"
    task :"upload:apim-key:store:#{account}" do
      include NHSx::Secret
      aae_config = aae_configuration($configuration, account)
      if check_file_exists(aae_config['apim_subscription_path'])
        Rake::Task["login:#{account}"].invoke
        apim_subscription_key = read_file_content_in(aae_config['apim_subscription_path'])
        store_aae_apim_subscription_key(service_name, consumer_name, apim_subscription_key, aae_config, $configuration)
      else
        puts "Key file: #{aae_config['apim_subscription_path']} not found this must exist and contain the apim subscription key"
      end
    end
  end

  desc 'Propagate the stored APIM subscription key for the AAE from dev to staging account'
  task :"upload:apim-key:propagate:staging" do
    include NHSx::Secret
    aae_config = aae_configuration($configuration, 'dev')

    Rake::Task['login:dev'].invoke
    download_aae_apim_subscription_key(service_name, consumer_name, aae_config, $configuration)
    apim_subscription_key = read_file_content_in(aae_config['apim_subscription_path'])

    Rake::Task['login:staging'].invoke
    store_aae_apim_subscription_key(service_name, consumer_name, apim_subscription_key, aae_config, $configuration)
  end
end
