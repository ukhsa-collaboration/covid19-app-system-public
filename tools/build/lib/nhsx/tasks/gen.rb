require "json"
require "digest"
namespace :gen do
  PROTOC = "protoc".freeze
  GA_PROTO = File.join($configuration.base, "src/proto/exposure.proto")
  desc "Generate the protobuf libraries"
  task :proto => [:"gen:proto:python", :"gen:proto:ruby"]

  desc "Generate the Ruby protobuf lib"
  task :"proto:ruby" do
    include Zuehlke::Execution
    output = File.join($configuration.base, "tools/protobuf/ruby/lib")
    mkdir_p(output, :verbose => false)
    cmdline = "#{PROTOC} -I=#{File.dirname(GA_PROTO)} --ruby_out=#{output} #{GA_PROTO}"
    run_command("Generate ruby exposure API classes", cmdline, $configuration)
  end

  desc "Generate the Python protobuf lib"
  task :"proto:python" do
    include Zuehlke::Execution
    output = File.join($configuration.base, "src/backend/lambda_functions/batch_zip")
    cmdline = "#{PROTOC} -I=#{File.dirname(GA_PROTO)}  --python_out=#{output} #{GA_PROTO}"
    run_command("Generate python exposure API classes", cmdline, $configuration)
  end

  desc "Generate the Java protobuf sources"
  task :"proto:java" do
    include Zuehlke::Execution

    protobuf_output = File.join($configuration.base, "src/aws/lambdas/incremental_distribution/cta/src/main/java")
    protobuf_lib = File.join(protobuf_output, "batchZipCreation")

    file protobuf_lib => [GA_PROTO] do
      mkdir_p(File.dirname(protobuf_lib), :verbose => false)
      cmdline = "#{PROTOC} -I=#{File.dirname(GA_PROTO)}  --java_out=#{protobuf_output} #{GA_PROTO}"
      run_command("Generate java exposure API protobuf classes", cmdline, $configuration)
    end
    Rake::Task[protobuf_lib].invoke
  end

  task :version do
    include Zuehlke::Git
    include Gaudi::Utilities
    write_file(File.join($configuration.out, "version.sha"), current_full_sha)
  end

  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.keys.each do |account|
    desc "Generate signatures for static content for the #{account} account"
    task :"signatures:#{account}" => [:"login:#{account}", :"gen:local_messages", :"build:dependencies"] do
      include Zuehlke::Execution
      include NHSx::TargetEnvironment
      include NHSx::AWS

      java_project_path = File.join($configuration.base, "src/aws/lambdas/incremental_distribution")
      mkdir_p(File.join($configuration.out, "signatures"), :verbose => false)

      Rake::FileList["#{$configuration.base}/src/static/*.json", "#{$configuration.out}/local-messages/local-messages.json"].each do |static_file|
        sig_file = File.join($configuration.out, "signatures", "#{File.basename(static_file)}.sig")
        date_file = File.join($configuration.out, "signatures", "#{File.basename(static_file)}.date")

        file sig_file => [static_file] do
          signature_data_json = File.join($configuration.out, "signatures", "#{File.basename(static_file)}.generated")
          sign_app_params = "--input #{static_file} --ssm-key-id #{NHSx::TargetEnvironment::CONTENT_SIGNING_KEY_PARAMETER} --output #{signature_data_json}"

          gradlew = File.join(java_project_path, "gradlew")
          cmdline = "#{gradlew} -p #{java_project_path} generateSignature -Dsign.args=\"#{sign_app_params}\""
          run_command("Generate signatures for #{File.basename(static_file)}", cmdline, $configuration)
          signatures = JSON.parse(File.read(signature_data_json))

          write_file(sig_file, signatures["Signature"])
          write_file(date_file, signatures["Signature-Date"])
        end
        Rake::Task[sig_file].invoke
      end
    end

    desc "Generates API keys for use in test for the #{account} account"
    task :"secrets:#{account}" => [:"login:#{account}"] do
      include NHSx::TargetEnvironment

      raise "Authentication headers are already configured. Run clean:test:secrets:#{account} to remove them" unless authentication_headers_for_test($configuration).empty?

      authorization_headers = {}
      NHSx::TargetEnvironment::API_NAMES.keys.each do |api_name|
        key_name = "used_for_tests_#{Digest::SHA1.hexdigest(Time.now.to_s)[0..6]}"
        authorization_header = create_and_store_api_key(api_name, key_name, "API key for use in test", $configuration)
        authorization_headers[api_name] = authorization_header
      end
      update_secrets_entry(NHSx::TargetEnvironment::TEST_API_KEY_HEADERS_SECRET, JSON.dump(authorization_headers).gsub("\"", "\\\""), $configuration)
    end
  end

  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      desc "Produce the environment config for the cta #{tgt_env} env"
      task :"config:#{tgt_env}" => [:"login:#{account}"] do
        include Zuehlke::Execution
        include NHSx::Generate
        generate_test_config(tgt_env, account, $configuration)
      end
    end
  end

  NHSx::TargetEnvironment::ANALYTICS_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      desc "Produce the environment config for the analytics #{tgt_env} env"
      task :"config:analytics:#{tgt_env}" => [:"login:#{account}"] do
        include Zuehlke::Execution
        include NHSx::Generate
        generate_analytics_test_config(tgt_env, account, $configuration)
      end
    end
  end

  desc "Generate the local messages payload. LA to message mapping can be overriden with MESSAGE_MAPPING"
  task :local_messages do
    include Zuehlke::Execution
    include NHSx::Generate

    mapping_file = $configuration.message_mapping($configuration)
    metadata_file = $configuration.messages_metadata($configuration)
    generate_local_messages(mapping_file, metadata_file, $configuration)
  end

  (NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS["dev"] | NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS["staging"]).each do |tgt_env|
    desc "Generate synthetic analytics for #{tgt_env}"
    task :"analytics:#{tgt_env}" do
      include Zuehlke::Execution

      raise GaudiError, "Generating synthetic analytics for env branch is not supported yet!" unless tgt_env != "branch"

      args = {
        'submission_parquet_bucket' => "te-#{tgt_env}-analytics-submission-parquet",
        'consolidated_submission_parquet_bucket' => "te-#{tgt_env}-analytics-consolidated-submission-parquet",
        'app_store_data_bucket' => "#{tgt_env}-analytics-app-store-qr-posters"
      }
      require "highline"
      cli = HighLine.new
      puts "\nRunning this task will delete and re-generate the contents of the following buckets: #{args.values}"
      answer = cli.ask "\nDo you want to proceed? Type 'yes' to confirm"
      raise GaudiError, "Aborted" unless ["yes"].include?(answer.downcase)

      args_cmdline = args.map { |k, v| "--#{k} #{v}"}.join(" ")
      cmdline = "python3 #{File.join($configuration.base, "tools/analytics/gen-parquet.py")} #{args_cmdline}"
      run_command("Generating parquet files", cmdline, $configuration)
    end
  end

  desc "Generate analytics fields in terraform source"
  task :"analytics-fields" => :"validate:analytics-fields" do
    include Zuehlke::Templates
    analytics_fields = File.join($configuration.base, "src/aws/analytics_fields/fields.json")
    columns = JSON.parse(File.read(analytics_fields))

    kinesis_glue_file = File.join($configuration.base, "src/aws/glue_tables.tf")
    kinesis_glue_template_file = "tools/templates/glue_tables_kinesis.tf.erb"
    kinesis_glue_template = File.join($configuration.base, kinesis_glue_template_file)
    write_file(kinesis_glue_file, from_template(kinesis_glue_template, { :columns => columns, :template_file => kinesis_glue_template_file }))

    quicksight_glue_file = File.join($configuration.base, "src/analytics/modules/mobile_analytics/glue_tables.tf")
    quicksight_glue_template_file = "tools/templates/glue_tables_quicksight.tf.erb"
    quicksight_glue_template = File.join($configuration.base, quicksight_glue_template_file)
    write_file(quicksight_glue_file, from_template(quicksight_glue_template, { :columns => columns, :template_file => quicksight_glue_template_file }))
  end
end
