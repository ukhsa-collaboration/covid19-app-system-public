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

  NHSx::TargetEnvironment::AAE_TARGET_ACCOUNTS.each do |account, aae_account|
    desc "Upload (create/update) new entries with certificates for mTLS against the AAE #{aae_account} account in Secrets Manager on #{account} account"
    task :"upload:configuration:#{account}" do
      include NHSx::Secret
      service_name = "aae"
      consumer_name = "advanced_analytics"
      aae_config = aae_configuration($configuration)

      if account == "staging"
        Rake::Task["login:dev"].invoke
        download_secrets(service_name, consumer_name, aae_config, $configuration)
        apim_subscription_key = read_file_content_in(aae_config["apim_subscription_path"])

        Rake::Task["login:staging"].invoke
        store_aae_x509_certificates(service_name, consumer_name, aae_config, $configuration)
        store_aae_pkcs12_certificates(service_name, consumer_name, aae_config, $configuration)
        store_aae_apim_subscription_key(service_name, consumer_name, apim_subscription_key, aae_config, $configuration)
      else
        Rake::Task["login:#{account}"].invoke
        apim_subscription_key = "placeholder"
        store_aae_apim_subscription_key(service_name, consumer_name, apim_subscription_key, aae_config, $configuration)

        create_aae_x509_certificate(aae_config, $configuration)
        store_aae_x509_certificates(service_name, consumer_name, aae_config, $configuration)

        create_aae_pkcs12_certificate(service_name, consumer_name, aae_config, $configuration)
        store_aae_pkcs12_certificates(service_name, consumer_name, aae_config, $configuration)
      end

      print_aae_certificate_issuer(aae_config, $configuration)
      print_aae_certificate_fingerprint(aae_config, $configuration)
      clean_aae_certificate(aae_config)
      puts "*" * 74
      puts "AWS Account User needs to share the public key with AAE Environment"
      puts aae_config["public_key_path"]
      puts "*" * 74

      secret_manager_arns = list_aae_advanced_analytics_secrets(service_name, consumer_name, $configuration)
      puts secret_manager_arns
    end
  end
end
