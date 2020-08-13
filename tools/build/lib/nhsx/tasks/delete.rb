namespace :delete do

  NHSx::TargetEnvironment::TARGET_ENVIRONMENTS["dev"].each do |tgt_env|
    desc "Delete the risky venues file in the S3 bucket of the #{tgt_env} target environment"
    task :"risky_venues:#{tgt_env}" do
      include NHSx::TargetEnvironment
      target_config = target_environment_configuration(tgt_env, "dev", $configuration)
      object_name = "#{target_config["risky_venues_distribution_store"]}/distribution/risky-venues"
      cmdline = NHSx::AWS::Commandlines.delete_from_s3(object_name)
      run_command("Delete risky venues", cmdline, $configuration)
    end
  end

end