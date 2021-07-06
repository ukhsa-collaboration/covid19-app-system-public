namespace :parquet do
  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      desc "Move default hive partition parquet files to a valid partition"
      task :"move:default_hive_partition:#{tgt_env}" => [:"login:#{account}"] do
        include NHSx::AWS
        prefix = target_environment_name(tgt_env, account, $configuration)
        s3_bucket = "#{prefix}-analytics-consolidated-submission-parquet"

        all_objects = list_objects(s3_bucket, "", $configuration)

        most_recent_partition = all_objects
                                  .filter_map { |object| object["Key"] if object["Key"] =~ /submitteddatehour=\d{4}-\d{2}-\d{2}-\d{2}\// }
                                  .sort
                                  .last
                                  .split("/")
                                  .first

        objects = list_objects(s3_bucket, "submitteddatehour=__HIVE_DEFAULT_PARTITION__", $configuration)
        hive_default_partition_objects = objects.filter_map { |object| object["Key"] if object["Key"].end_with?(".parquet") }

        hive_default_partition_objects.each { |object_key|
          source_uri = "s3://#{s3_bucket}/#{object_key}"
          destination_uri = "s3://#{s3_bucket}/#{most_recent_partition}/#{object_key.sub("submitteddatehour=__HIVE_DEFAULT_PARTITION__/", "")}"
          move_s3_file(source_uri, destination_uri, $configuration)
        }
      end
    end
  end
end
