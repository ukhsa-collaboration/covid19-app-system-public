namespace :download do
  include NHSx::TargetEnvironment
  namespace :data do
    NHSx::TargetEnvironment::TARGET_ENVIRONMENTS["dev"].each do |tgt_env|
      post_districts_out_dir = File.join($configuration.out, "gen/post_districts")
      desc "Download tier meta data to #{post_districts_out_dir}"
      task :"tier_metadata:#{tgt_env}" do
        target_config = target_environment_configuration(tgt_env, "dev", $configuration)
        file_name = "tier-metadata"
        distribution_store = "post_districts_distribution_store"
        object_name = "#{target_config[distribution_store]}/#{file_name}"
        local_target = File.join(post_districts_out_dir, file_name)
        run_command("Download tier meta data of #{tgt_env} deployment", NHSx::AWS::Commandlines.download_from_s3(object_name, local_target), $configuration)
      end
    end    
  end  
end