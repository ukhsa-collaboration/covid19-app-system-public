require "json"
namespace :migrate do
  namespace :submissions do
    include NHSx::TargetEnvironment
    NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        prerequisites = [:"login:#{account}"]
        desc "Migrate json submissions on #{tgt_env} target environment"
        task :"#{tgt_env}" => prerequisites do
          target_environment_config = target_environment_configuration(tgt_env, account, $configuration)
          bucket_name = target_environment_config["diagnosis_keys_submission_store"]
          bucket_objects = get_objects(bucket_name)
          bucket_objects.each do |object|
            object_name = object["Key"]
            new_object_name = rename(object_name)
            migrate_submission(bucket_name, object_name, new_object_name)
          end
        end

        def get_objects(bucket_name)
          command = "aws s3api list-objects --bucket #{bucket_name} --delimiter /"
          cmd = run_command("get objects in bucket",command, $configuration)
          return JSON.parse(cmd.output)["Contents"]
        end

        def rename(name)
          timestamp = name.split("_")[0].to_i
          guid = name.split("_")[1]
          reformatted_timestamp = Time.at(timestamp/1000).strftime('%Y%m%d');
          return reformatted_timestamp + "/" + guid
        end

        def migrate_submission(bucket, object_name, new_object_name)
          prefix = "mobile"
          cmdline = "aws s3 mv s3://#{bucket}/#{object_name} s3://#{bucket}/#{prefix}/#{new_object_name} "
          run_command("#{object_name} migrated to #{new_object_name}", cmdline, $configuration)
        end
      end
    end
  end
end
