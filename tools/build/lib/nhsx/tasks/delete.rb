namespace :delete do
  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS["dev"].each do |tgt_env|
    desc "Delete the risky venues file in the S3 bucket of the #{tgt_env} target environment"
    task :"risky_venues:#{tgt_env}" do
      include NHSx::TargetEnvironment
      target_config = target_environment_configuration(tgt_env, "dev", $configuration)
      object_name = "#{target_config["risky_venues_distribution_store"]}/distribution/risky-venues"
      cmdline = NHSx::AWS::Commandlines.delete_from_s3(object_name)
      run_command("Delete risky venues", cmdline, $configuration)
    end
  end
  namespace :parquet_consolidation_job do
    NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        desc "delete a parquet consolidation job"
        task :"#{tgt_env}" => [:"login:#{account}"] do
          prefix = target_environment_name(tgt_env, account, $configuration)
          job_name = "#{prefix}-parquet-consolidation"
          s3_bucket = "#{prefix}-temporary-analytics-consolidated-submission-parquet"

          cmdline = "aws glue delete-job --job-name #{job_name}"
          run_command("Deleting glue job", cmdline, $configuration)

          NHSx::AWS::Commandlines.empty_s3_bucket(s3_bucket, $configuration)
          cmdline = NHSx::AWS::Commandlines.delete_bucket("s3://#{s3_bucket}")
          run_command("Delete parquet consolidation bucket", cmdline, $configuration)
        end
      end
    end
  end
  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      prerequisites = [:"login:#{account}"]
      desc "Delete the local stats v1 file in the S3 bucket of the #{tgt_env} target environment"
      task :"local_stats:#{tgt_env}" => prerequisites do
        include NHSx::TargetEnvironment
        target_config = target_environment_configuration(tgt_env, account, $configuration)
        object_name = "#{target_config["local_stats_distribution_store"]}/distribution/v1/local-covid-stats-daily"
        cmdline = NHSx::AWS::Commandlines.delete_from_s3(object_name)
        run_command("Delete v1 local stats file", cmdline, $configuration)
      end
    end
  end
end
