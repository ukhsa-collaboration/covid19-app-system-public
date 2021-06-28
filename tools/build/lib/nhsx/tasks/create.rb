require "json"
require "shellwords"

namespace :create do
  namespace :ipc do
    NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        desc "Create and update an isolation payment token for England"
        task :"token:en:#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::API::Submission

          target_config = target_environment_configuration(tgt_env, account, $configuration)
          ipc_token = create_isolation_payment_token("England", target_config, $configuration)

          update_isolation_payment_token(ipc_token, target_config, $configuration)
          puts "Created and updated IPC token #{ipc_token}"
        end
        desc "Create and update an isolation payment token for Wales"
        task :"token:wa:#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::API::Submission

          target_config = target_environment_configuration(tgt_env, account, $configuration)
          ipc_token = create_isolation_payment_token("Wales", target_config, $configuration)

          update_isolation_payment_token(ipc_token, target_config, $configuration)
          puts "Created and updated IPC token #{ipc_token}"
        end
      end
    end
  end
  namespace :parquet_consolidation_job do
    NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        desc "Create a parquet consolidation job"
        task :"#{tgt_env}" => [:"login:#{account}"] do
          prefix = target_environment_name(tgt_env, account, $configuration)
          job_name = "#{prefix}-parquet-consolidation"
          s3_bucket = "s3://#{prefix}-temporary-analytics-consolidated-submission-parquet"
          script_location = "tools/parquet-consolidation/SparkRead-v1.0.py"
          parquet_script = File.join($configuration.base, script_location)
          object_name = "#{prefix}-temporary-analytics-consolidated-submission-parquet/SparkRead-v1.0.py"
          destination_bucket = "s3://#{prefix}-analytics-consolidated-submission-parquet"
          source_bucket = "s3://#{prefix}-analytics-submission-parquet"

          cmdline = NHSx::AWS::Commandlines.create_bucket(s3_bucket)
          run_command("Create parquet consolidation bucket", cmdline, $configuration)

          cmdline = NHSx::AWS::Commandlines.upload_to_s3(parquet_script, object_name, "text/x-python")
          run_command("Upload glue script to s3 bucket", cmdline, $configuration)

          default_arguments = {"--job-bookmark-option" => "job-bookmark-enable",
                               "--enable-metrics" => "",
                               "--enable-continuous-cloudwatch-log" => "true",
                               "--encryption-type": "sse-s3",
                               "--TempDir" => s3_bucket,
                               "--SOURCE_BUCKET_URI" => source_bucket,
                               "--DESTINATION_BUCKET_URI" => destination_bucket}

          cmdline = "aws glue create-job --name #{job_name} --role #{AWS_DEPLOYMENT_ROLES[account].gsub(/\/.*$/, "/GlueServiceRole")} " +
              "--command '{ \"Name\": \"glueetl\", \"ScriptLocation\": \"s3://#{object_name}\"}' " +
              "--number-of-workers 10 --worker-type G.2X --region eu-west-2 --glue-version 2.0 " +
              "--default-arguments #{Shellwords.escape(JSON.dump(default_arguments))}"

          run_command("Create parquet consolidation job", cmdline, $configuration)
        end
      end
    end
  end
end


