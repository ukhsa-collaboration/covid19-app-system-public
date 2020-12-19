require "json"

module NHSx
  # Helpers that codify the use of the AWS CLI within the NHSx project
  module AWS
    # The default region
    AWS_REGION = "eu-west-2".freeze
    # The AWS profile to use when authenticating with MFA
    AWS_AUTH_PROFILE = "nhs-auth".freeze
    # The full AWS role ARNs to use when logging in for deployment
    AWS_DEPLOYMENT_ROLES = {
      "staging" => "arn:aws:iam::123456789012:role/staging-ApplicationDeploymentUser",
      "prod" => "arn:aws:iam::123456789012:role/prod-ApplicationDeploymentUser",
    }.freeze
    # The full AWS role ARNs to use when logging in for queries
    AWS_READ_ROLES = {
      "staging" => "arn:aws:iam::123456789012:role/staging-ReadOnlyUser",
      "prod" => "arn:aws:iam::123456789012:role/prod-ReadOnlyUser",
    }.freeze
    # The user friendly names for the AWS roles
    AWS_ROLE_NAMES = ["deploy", "read"].freeze
    # AWS CLI command lines in use by automation scripts
    module Commandlines

      # Creates a new output files to be passed into the lambda invoke funtion
      def self.new_lambda_output_file(lambda_function, system_config)
        outdir = File.join(system_config.out, "/logs/lambdas")
        mkdir_p(outdir, :verbose => false)
        return "#{outdir}/#{Time.now.strftime("%Y%m%d_%H%M%S")}_#{lambda_function}.log"
      end

      # Invoke an AWS Lambda function by name and put the response under out/logs/lambdas
      def self.invoke_lambda(lambda_function, payload, output_file)
        payload_cmd = "--cli-binary-format raw-in-base64-out --payload #{payload}" unless payload.empty?
        "aws --cli-read-timeout 0 --cli-connect-timeout 0 lambda invoke --region #{AWS_REGION} --function-name #{lambda_function} #{payload_cmd} #{output_file}"
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

      # Upload an object to S3 from the local source.
      #
      # object_name is the full path to the object (including the bucket name)
      def self.upload_to_s3(local_source, object_name, content_type)
        "aws s3 cp --content-type #{content_type} #{local_source} s3://#{object_name}"
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

      def self.get_active_synth_lambda_layers(region, system_config)
        cmdline = "aws lambda list-functions --region #{region}"
        cmd = run_command("Retrieve list of active synth lambda layers", cmdline, system_config)
        active_synth_lambda_layers = []
        lambda_layers = JSON.parse(cmd.output)["Functions"].map { |el| el["Layers"] }
        lambda_layers.map do |layers_list|
          if !layers_list
            next
          end
          layers_list.map do |layer|
            if layer["Arn"].match(/:(cwsyn-.*):[^:]*$/)
              layer_name = layer["Arn"].match(/:(cwsyn-.*):[^:]*$/).captures[0]
              if !active_synth_lambda_layers.include?(layer_name)
                active_synth_lambda_layers.push(layer_name)
              end
            end
          end
        end
        active_synth_lambda_layers
      end

      def self.get_all_synth_lambda_layers(region, system_config)
        cmdline = "aws lambda list-layers --region #{region}"
        cmd = run_command("Retrieve list of all synth lambda layers", cmdline, system_config)
        JSON.parse(cmd.output)["Layers"].select { |el| el["LayerName"].start_with?("cwsyn-") }.map { |el| el["LayerName"] }
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
    end

    def ssm_parameter(parameter_name, system_config)
      cmdline = NHSx::AWS::Commandlines.get_ssm_parameter(parameter_name)
      cmd = run_command("Retrieve #{parameter_name.split("/").last}", cmdline, system_config)
      parameter_data = JSON.parse(cmd.output.chomp)
      return parameter_data["Parameter"]["Value"]
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

    # Perform an aws-mfa login to prompt for the MFA code
    def mfa_login(role_name, account)
      role_arn = aws_role_arn(role_name, account)
      cmdline = NHSx::AWS::Commandlines.multi_factor_authentication(NHSx::AWS::AWS_AUTH_PROFILE, role_arn, account)
      sh(cmdline)
    end

    def get_orphaned_synthetics_lambda_layers(region, system_config)
      all_synth_lambda_layers = NHSx::AWS::Commandlines.get_all_synth_lambda_layers(region, system_config)
      active_synth_lambda_layers = NHSx::AWS::Commandlines.get_active_synth_lambda_layers(region, system_config)
      all_synth_lambda_layers - active_synth_lambda_layers
    end

    def get_lambda_layer_versions(layer_name, region, system_config)
      cmd = run_command("Retrieve list of lambda layer versions",
                        NHSx::AWS::Commandlines.get_lambda_layer_versions(layer_name, region), system_config)
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

    # Map a user friendly role name we can add as a parameter to the tasks to the full ARN
    def aws_role_arn(role_name, account)
      case role_name
      when "deploy"
        AWS_DEPLOYMENT_ROLES[account]
      when "read"
        AWS_READ_ROLES[account]
      else
        raise GaudiError, "No ARN corresponding to #{role_name}"
      end
    end
  end
end
