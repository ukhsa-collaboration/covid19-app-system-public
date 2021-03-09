namespace :clean do
  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each_key do |account|
    desc "Delete orphaned synthetics lambda layers from #{account} account"
    task :"synth:#{account}" => [:"login:#{account}"] do
      include NHSx::AWS_Synth
      region = "eu-west-1"
      delete_orphan_synth_resources(region, $configuration)
    end
  end
end
