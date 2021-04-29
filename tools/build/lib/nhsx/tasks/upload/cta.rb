namespace :upload do
  namespace :cta do
    namespace :edge do
      NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, _tgt_envs|
        prerequisites = [:"login:#{account}"]
        desc "Upload to #{account} Secrets Manager"
        task :"#{account}" => prerequisites do
          include NHSx::Secret
          service_name = "edge"
          consumer_name = "azure_storage_container"
          edge_config = edge_configuration($configuration)
          edge_config["sas_token"] = "replace-me-with-the-real-token"
          store_sas_token(service_name, consumer_name, edge_config, $configuration)
        end
      end
    end
  end
end
