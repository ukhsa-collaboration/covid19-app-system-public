namespace :migrate do
  ###### To be deleted after the analytics migration ######

   ANALYTICS_MIGRATION_TARGET_ENVIRONMENTS = {
     "dev" => "aa-dev",
     "staging" => "aa-staging",
     "prod" => "aa-prod"
   }.freeze

  namespace :edge do
        namespace :analytics do
            namespace :secrets do
                 ANALYTICS_MIGRATION_TARGET_ENVIRONMENTS.each do |source_account, target_account|
                  desc "migrate EDGE analytics export secrets from #{source_account} to #{target_account}"
                  task :"#{source_account}" do
                    include NHSx::Secret
                    service_name = "edge"
                    consumer_name = "azure_storage_container"
                    edge_config = edge_configuration($configuration)
                    with_account(source_account,"analytics",$configuration) do
                        mkdir_p("out/secrets")
                        download_edge_sas_token($configuration, edge_config)
                    end
                    with_account(target_account,"analytics",$configuration) do
                       store_sas_token(service_name, consumer_name, edge_config, $configuration)
                       rm_r("out/secrets")
                    end
                  end
                end
            end
        end
  end

  namespace :aae do
     namespace :analytics do
         namespace :secrets do
             ANALYTICS_MIGRATION_TARGET_ENVIRONMENTS.each do |source_account, target_account|
               desc "migrate AAE analytics export secrets from #{source_account} to #{target_account}"
               task :"#{source_account}" do
                 include NHSx::Secret
                 service_name = "aae"
                 consumer_name = "advanced_analytics"
                 aae_config = aae_configuration($configuration)
                 with_account(source_account,"analytics",$configuration) do
                    download_secrets(service_name, consumer_name, aae_config, $configuration)
                 end
                 with_account(target_account,"analytics",$configuration) do
                    store_aae_x509_certificates(service_name, consumer_name, aae_config, $configuration)
                    store_aae_pkcs12_certificates(service_name, consumer_name, aae_config, $configuration)
                    apim_subscription_key = read_file_content_in(aae_config["apim_subscription_path"])
                    store_aae_apim_subscription_key(service_name, consumer_name, apim_subscription_key, aae_config, $configuration)
                    rm_r("out/certificates")
                 end
               end
             end
         end
     end
  end
end
