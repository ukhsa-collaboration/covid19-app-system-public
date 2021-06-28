require "json"
require "shellwords"
module NHSx
  # Helpers that codify the use of the AWS CLI within the NHSx project
  module AWS
    AWS_CONFIG_PATHS = {
      "config" => "#{ENV["HOME"]}/.aws/config",
      "credentials" => "#{ENV["HOME"]}/.aws/credentials",
    }.freeze
    # The default region
    AWS_REGION = "eu-west-2".freeze
    # AWS CLI command lines in use by automation scripts
    module Commandlines
      # Creates a new output files to be passed into the lambda invoke funtion
      def self.new_lambda_output_file(lambda_function, system_config)
        outdir = File.join(system_config.out, "/logs/lambdas")
        mkdir_p(outdir, :verbose => false)
        "#{outdir}/#{Time.now.strftime("%Y%m%d_%H%M%S")}_#{lambda_function}.log"
      end

      # Invoke an AWS Lambda function by name and put the response under out/logs/lambdas
      def self.invoke_lambda(lambda_function, payload, output_file)
        payload_cmd = "--cli-binary-format raw-in-base64-out --payload #{Shellwords.escape(payload)}" unless payload.empty?
        "aws --cli-read-timeout 0 --cli-connect-timeout 0 lambda invoke --region #{AWS_REGION} --function-name #{lambda_function} #{payload_cmd} #{output_file}"
      end

      #Execute an Athena query
      def self.start_athena_query(query, database, workgroup, region = AWS_REGION)
        "aws athena start-query-execution --query-string \"#{query}\" --query-execution-context Database=#{database} --work-group #{workgroup} --region #{region}"
      end

      def self.get_athena_named_query(query_id, region = AWS_REGION)
        "aws athena get-named-query --named-query-id #{query_id} --region #{region}"
      end

      # Retrieve a temporary ECR authentication token
      def self.ecr_login(region = AWS_REGION)
        "aws ecr get-login-password --region #{region}"
      end

      # Download an object from S3 into the local_target file.
      #
      # object_name is the full path to the object (including the bucket name)
      def self.download_from_s3(object_name, local_target)
        "aws s3 cp s3://#{object_name} #{local_target}"
      end

      # Download, recursively an object from S3 into the local_target file.
      def self.download_from_s3_recursively(object_name, local_target)
        "aws s3 cp s3://#{object_name} #{local_target} --recursive"
      end

      # Upload an object to S3 from the local source.
      #
      # object_name is the full path to the object (including the bucket name)
      def self.upload_to_s3(local_source, object_name, content_type)
        c_type = content_type ? " --content-type #{content_type}" : ""
        "aws s3 cp #{local_source} s3://#{object_name}#{c_type}"
      end

      # Upload, recursively, to S3 from the local source.
      #
      # object_name is the full path to the object (including the bucket name)
      def self.upload_to_s3_recursively(local_source, object_name)
        "aws s3 cp #{local_source} s3://#{object_name} --recursive"
      end

      # Delete an object from S3.
      #
      # object_name is the full path to the object (including the bucket name)
      def self.delete_from_s3(object_name)
        "aws s3 rm s3://#{object_name}"
      end

      # Delete recursively from S3.
      def self.delete_from_s3_recursively(object_name)
        "aws s3 rm s3://#{object_name} --recursive"
      end

      def self.create_bucket(s3_uri)
        "aws s3 mb #{s3_uri}"
      end

      def self.delete_bucket(s3_uri)
        "aws s3 rb #{s3_uri}"
      end

      def self.list_objects(bucket_name, prefix)
        "aws s3api list-objects-v2 --bucket #{bucket_name} --prefix \"#{prefix}\""
      end

      # Download the public key in .der format for the given key_id into public_key
      def self.download_public_key(key_id, public_key)
        "aws kms get-public-key --key-id #{key_id} --query PublicKey --region #{AWS_REGION} | tail -c +2 | head -c -2 | base64 --decode > #{public_key}"
      end

      # Signs a specific message digest from a path
      def self.sign_digest_from(server_key_arn, message_digest_path)
        "aws kms sign --key-id #{server_key_arn} --signing-algorithm ECDSA_SHA_256 --message-type DIGEST --message fileb://#{message_digest_path} --output text --query Signature --region #{AWS_REGION}"
      end

      # Verifies signature of a specific message digest (both in binary format)
      def self.verify_digest_signature(server_key_arn, message_digest_path, signature)
        "aws kms verify --key-id #{server_key_arn} --signing-algorithm ECDSA_SHA_256 --message-type DIGEST --message fileb://#{message_digest_path} --signature #{signature} --region #{AWS_REGION}"
      end

      # Retrieves the value of an SSM parameter
      def self.get_ssm_parameter(parameter_name)
        "aws ssm get-parameter --name #{parameter_name} --region #{AWS_REGION}"
      end

      # Retrieves a secret from the secrets manager
      def self.retrieve_secret(secret_name)
        "aws secretsmanager get-secret-value --secret-id #{secret_name} --region #{AWS_REGION}"
      end

      # Deletes a secret from the secrets manager
      def self.delete_secret(secret_name)
        "aws secretsmanager delete-secret --secret-id #{secret_name} --region #{AWS_REGION}"
      end

      # List all the secrets in SecretsManager
      def self.all_secrets
        "aws secretsmanager list-secrets  --region #{AWS_REGION}"
      end

      # Deletes a secret from the secrets manager
      def self.update_secret(secret_name, string_value)
        "aws secretsmanager put-secret-value --secret-id #{secret_name} --secret-string \"#{string_value}\" --region #{AWS_REGION}"
      end

      # The command line for aws-mfa that creates a temporary authentication token for aws CLI
      def self.multi_factor_authentication(profile, role, suffix)
        "aws-mfa --duration 3600 --profile #{profile} --assume-role #{role} --long-term-suffix none --short-term-suffix #{suffix}"
      end

      def self.sso_login(sso_profile)
        "aws sso login --profile #{sso_profile}"
      end

      def self.sts_assume_role(profile, role, account_number, session_name)
        "aws sts assume-role --profile=#{profile} --role-arn arn:aws:iam::#{account_number}:role/#{role} --role-session-name=#{session_name}"
      end

      def self.get_lambda_layer_versions(layer_name, region)
        "aws lambda list-layer-versions --layer-name #{layer_name} --region #{region}"
      end

      def self.delete_lambda_layer_version(layer_name, version_number, region)
        "aws lambda delete-layer-version --region #{region} --version-number #{version_number} --layer-name #{layer_name}"
      end

      def self.update_ssm_parameter(parameter_name, value)
        "aws ssm put-parameter --name #{parameter_name} --value #{value} --overwrite"
      end

      def self.update_lambda_env_vars(function_name, environment)
        "aws lambda update-function-configuration --function-name #{function_name} --environment '#{environment}'"
      end

      def self.get_function_configuration(function_name, region)
        "aws lambda get-function-configuration --function-name #{function_name} --region #{region}"
      end

      def self.list_event_source_mappings(function_name)
        "aws lambda list-event-source-mappings --function-name #{function_name}"
      end

      def self.enable_event_source_mapping(uuid)
        "aws lambda update-event-source-mapping --uuid #{uuid} --enabled"
      end

      def self.disable_event_source_mapping(uuid)
        "aws lambda update-event-source-mapping --uuid #{uuid} --no-enabled"
      end

      def self.delete_lambda_function(function_name, region)
        "aws lambda delete-function --function-name #{function_name} --region #{region}"
      end

      def self.delete_log_group(log_group_name, region)
        "aws logs delete-log-group --log-group-name #{log_group_name} --region #{region}"
      end

      def self.list_lambda_layers(region)
        "aws lambda list-layers --region #{region}"
      end

      def self.list_log_groups(prefix, region)
        "aws logs describe-log-groups --log-group-name-prefix #{prefix} --region #{region}"
      end

      def self.list_lambda_functions(region)
        "aws lambda list-functions --region #{region}"
      end

      # Lists all S3 buckets known to Terraform in the workspace, extracts their AWS identifier and recursively deletes
      def self.empty_workspace_buckets
        'terraform refresh $(terraform state list | grep "aws_s3_bucket\\." ' +
          '| sed "s/^\\(.*\\)$/ -target=\\1/") | grep -o "id=[^]]*" ' +
          '| sed "s/^id=\\(.*\\)$/aws s3 rm s3:\\/\\/\\1 --recursive/" | bash > /dev/null'
      end
    end # of module Commandlines

    def ssm_parameter(parameter_name, system_config)
      cmdline = NHSx::AWS::Commandlines.get_ssm_parameter(parameter_name)
      cmd = run_command("Retrieve #{parameter_name.split("/").last}", cmdline, system_config)
      parameter_data = JSON.parse(cmd.output.chomp)
      parameter_data["Parameter"]["Value"]
    end

    # Return the secret for secret_name
    def secrets_entry(secret_name, system_config)
      cmd = run_command("Retrieve #{secret_name}", NHSx::AWS::Commandlines.retrieve_secret(secret_name), system_config)
      JSON.parse(cmd.output).fetch("SecretString", "")
    end

    # Return the list of all the names for the secrets in the SecretsManager
    def all_secrets(system_config)
      cmd = run_command("Retrieve list of secrets", NHSx::AWS::Commandlines.all_secrets, system_config)
      JSON.parse(cmd.output)["SecretList"].map { |el| el["Name"] }
    end

    def update_secrets_entry(secret_name, string_secret, system_config)
      run_command("Update #{secret_name}", NHSx::AWS::Commandlines.update_secret(secret_name, string_secret), system_config)
    end

    def delete_lambda_functions(functions, region, system_config)
      functions.map do |function_name|
        delete_lambda_function(function_name, region, system_config)
      end
    end

    def delete_log_groups(log_groups, region, system_config)
      log_groups.map do |log_group_name|
        delete_log_group(log_group_name, region, system_config)
      end
    end

    def delete_lambda_function(function_name, region, system_config)
      cmdline = NHSx::AWS::Commandlines.delete_lambda_function(function_name, region)
      sh(cmdline)
    end

    def delete_log_group(log_group_name, region, system_config)
      cmdline = NHSx::AWS::Commandlines.delete_log_group(log_group_name, region)
      sh(cmdline)
    end

    def delete_lambda_layers(layers, region, system_config)
      layers.each do |layer_name|
        layer_versions = get_lambda_layer_versions(layer_name, region, system_config)
        layer_versions.each do |layer_version|
          delete_lambda_layer_version(layer_name, layer_version, region)
        end
      end
    end

    def get_lambda_layer_versions(layer_name, region, system_config)
      cmdline = NHSx::AWS::Commandlines.get_lambda_layer_versions(layer_name, region)
      cmd = run_command("Retrieve list of lambda layer versions for #{layer_name}", cmdline, system_config)
      JSON.parse(cmd.output)["LayerVersions"].map { |el| el["Version"] }
    end

    def delete_lambda_layer_version(layer_name, version_number, region)
      cmdline = NHSx::AWS::Commandlines.delete_lambda_layer_version(layer_name, version_number, region)
      sh(cmdline)
    end

    def update_ssm_parameter(name, value, system_config)
      cmd = NHSx::AWS::Commandlines.update_ssm_parameter(name, value)
      run_command("Update #{name} ssm parameter with value #{value}", cmd, system_config)
    end

    def update_lambda_env_vars(function_name, variables, system_config)
      full_var_dict = get_lambda_env_vars(function_name, system_config).merge(variables)
      environment = JSON.generate({ "Variables" => full_var_dict })
      cmdline = NHSx::AWS::Commandlines.update_lambda_env_vars(function_name, environment)
      run_command("Update #{function_name} lambda function environment variables with values: #{variables}", cmdline, system_config)
    end

    def get_lambda_env_vars(function_name, system_config)
      cmdline = NHSx::AWS::Commandlines.get_function_configuration(function_name, AWS_REGION)
      cmd = run_command("Get function configuration for #{function_name} lambda function", cmdline, system_config)
      JSON.parse(cmd.output)["Environment"]["Variables"]
    end

    def list_event_source_mappings(function_name, system_config)
      cmdline = NHSx::AWS::Commandlines.list_event_source_mappings(function_name)
      cmd = run_command("List even source mappings", cmdline, system_config)
      JSON.parse(cmd.output)["EventSourceMappings"]
    end

    def get_event_source_mapping_uuid(function_name, event_source_arn, function_arn, system_config)
      mappings = list_event_source_mappings(function_name, system_config)
      mappings.find { |mapping| mapping["EventSourceArn"] == event_source_arn and mapping["FunctionArn"] == function_arn }["UUID"]
    end

    def enable_event_source_mapping(uuid, system_config)
      cmdline = NHSx::AWS::Commandlines.enable_event_source_mapping(uuid)
      run_command("Enabling event source mapping: #{uuid}", cmdline, system_config)
    end

    def disable_event_source_mapping(uuid, system_config)
      cmdline = NHSx::AWS::Commandlines.disable_event_source_mapping(uuid)
      run_command("Disabling event source mapping: #{uuid}", cmdline, system_config)
    end

    def get_lambda_custom_oai(lambda_function, system_config)
      env_vars = get_lambda_env_vars(lambda_function, system_config)
      custom_oai = env_vars["custom_oai"]
      raise GaudiError, "Custom oai not found for #{lambda_function}" unless custom_oai
      custom_oai
    end

    # Deletes the contents of any S3 buckets in the Terraform workspace corresponding to the given name
    def empty_workspace_buckets(workspace_name, terraform_configuration, system_config)
      workspace_id = select_workspace(workspace_name, terraform_configuration, system_config)
      Dir.chdir(terraform_configuration) do
        begin
          # Buckets with a lot of files in them can take too long to delete via Terraform,
          # so we pre-emptively delete the contents via the command line
          run_tee("Empty buckets in #{workspace_id}", NHSx::AWS::Commandlines.empty_workspace_buckets, system_config)
          # The above code is removed from delete_workspace()
        end
      end
    end

    # Finds the content-type matching on the file extension or nil if content type is not found
    def content_type_of(file_path)
      file_extension = File.extname(file_path)
      case file_extension
      when ".csv"
        "text/csv"
      when ".json"
        "application/json"
      else
        nil
      end
    end

    def upload_single_file_to_s3(file_path, s3_location, system_config)
      content_type = content_type_of(file_path)
      cmdline = NHSx::AWS::Commandlines.upload_to_s3(file_path, s3_location, content_type)
      run_command("Uploading #{file_path} to #{s3_location}", cmdline, system_config)
    end

    def upload_recursively_to_s3(local_dir, s3_location, system_config)
      cmdline = NHSx::AWS::Commandlines.upload_to_s3_recursively(local_dir, s3_location)
      run_command("Uploading #{local_dir} to #{s3_location}", cmdline, system_config)
    end

    def download_recursively_from_s3(s3_location, local_dir, system_config)
      cmdline = NHSx::AWS::Commandlines.download_from_s3_recursively(s3_location, local_dir)
      run_command("Downloading #{s3_location} to #{local_dir}", cmdline, system_config)
    end

    def list_objects(bucket_name, prefix, system_config)
      cmdline = NHSx::AWS::Commandlines.list_objects(bucket_name, prefix)
      cmd = run_command("List objects for #{bucket_name} with prefix #{prefix}", cmdline, system_config)
      cmd.output.empty? ? [] : JSON.parse(cmd.output)["Contents"]
    end

    def invoke_lambda(lambda_function, payload, system_config)
      output_log_file = NHSx::AWS::Commandlines.new_lambda_output_file(lambda_function, system_config)
      cmd_line = NHSx::AWS::Commandlines.invoke_lambda(lambda_function, payload, output_log_file)
      run_command("Invoke #{lambda_function} lambda", cmd_line, system_config)
    end

    def empty_s3_bucket(s3_location, system_config)
      cmdline = NHSx::AWS::Commandlines.delete_from_s3_recursively(s3_location)
      run_command("Deleting recursively #{s3_location}", cmdline, system_config)
    end
  end
end
