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

    protobuf_output = File.join($configuration.base, "src/aws/lambdas/incremental_distribution/src/main/java")
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

  NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.keys.each do |account|
    desc "Generate signatures for static content for the #{account} account"
    task :"signatures:#{account}" => [:"login:#{account}", :"build:dependencies"] do
      include Zuehlke::Execution
      include NHSx::TargetEnvironment
      include NHSx::AWS

      java_project_path = File.join($configuration.base, "src/aws/lambdas/incremental_distribution")
      pom_xml_path = File.join(java_project_path, "pom.xml")
      java_output_path = File.join($configuration.out, "java/batch_creation")
      mkdir_p(File.join($configuration.out, "signatures"), :verbose => false)

      Rake::FileList["#{$configuration.base}/src/static/*.json"].each do |static_file|
        sig_file = File.join($configuration.out, "signatures", "#{File.basename(static_file)}.sig")
        date_file = File.join($configuration.out, "signatures", "#{File.basename(static_file)}.date")

        file sig_file => [static_file] do
          signature_data_json = File.join($configuration.out, "signatures", "#{File.basename(static_file)}.generated")
          sign_app_params = "--input #{static_file} --ssm-key-id #{NHSx::TargetEnvironment::CONTENT_SIGNING_KEY_PARAMETER} --output #{signature_data_json}"
          cmdline = "mvn -f=#{pom_xml_path} -DbuildOutput=#{java_output_path} exec:java -Dexec.mainClass=\"uk.nhs.nhsx.core.signature.DistributionSignatureMain\"  -Dexec.args=\"#{sign_app_params}\" "
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

      api_names = ["mobile", "testResultUpload", "highRiskVenuesCodeUpload", "highRiskPostCodeUpload"] # see uk.nhs.nhsx.core.auth.ApiName

      authorization_headers = {}
      api_names.each do |api_name|
        key_name = "used_for_tests_#{Digest::SHA1.hexdigest(Time.now.to_s)[0..6]}"
        authorization_header = create_and_store_api_key(api_name, key_name)
        authorization_headers[api_name] = authorization_header
      end
      update_secrets_entry(NHSx::TargetEnvironment::TEST_API_KEY_HEADERS_SECRET, JSON.dump(authorization_headers).gsub("\"", "\\\""), $configuration)
    end
  end

  NHSx::TargetEnvironment::TARGET_ENVIRONMENTS["dev"].each do |tgt_env|
    desc "Generate signatures for static content for the #{tgt_env} env"
    task :"config:#{tgt_env}" do
      include Zuehlke::Execution
      include NHSx::Generate
      generate_test_config(tgt_env, "dev", $configuration)
    end
  end

end
