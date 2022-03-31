require 'shellwords'
require 'bcrypt'

module NHSx
  # Helper methods for creating api keys
  module Secret
    include Zuehlke::Execution

    def create_secret
      'aws secretsmanager create-secret --name'
    end

    def secret_value
      "aws secretsmanager get-secret-value --region #{NHSx::AWS::AWS_REGION} --secret-id"
    end

    def put_secret
      'aws secretsmanager put-secret-value --secret-id'
    end

    def list_secrets_arn(by_secrets_path)
      "aws secretsmanager list-secrets --region #{NHSx::AWS::AWS_REGION} --filters Key=name,Values=[\"#{by_secrets_path}/\"] --query 'SecretList[*].ARN' --output json"
    end

    def store_secret_binary(name, secret, description, system_config, aws_region = NHSx::AWS::AWS_REGION)
      # Warning: a secret can only be deleted after a minimum period of 7 days
      # This script may fail if the secret name already exists but is pending deletion
      cmdline = "#{create_secret} #{name} --secret-binary '#{secret}' --region #{aws_region} --description '#{description}'"
      begin
        run_quiet('Create aws secret', cmdline, system_config)
      rescue GaudiError => failure
        if /ResourceExistsException/.match(failure.message)
          puts("Failed to create secret: #{name}\nAttempting to update it")
          cmdline = "#{put_secret} #{name} --secret-binary '#{secret}' --region #{aws_region}"
          run_quiet('Update aws secret', cmdline, system_config)
          puts "Secret updated successfully"
          puts "\n"
        else
          raise failure # re-throw - nothing we can do
        end
      end
    end

    def store_secret_string(name, secret, description, system_config, aws_region = NHSx::AWS::AWS_REGION)
      # Warning: a secret can only be deleted after a minimum period of 7 days
      # This script may fail if the secret name already exists but is pending deletion
      cmdline = "#{create_secret} #{name} --secret-string '#{secret}' --region #{aws_region} --description '#{description}'"
      begin
        run_quiet('Create aws secret', cmdline, system_config)
      rescue GaudiError => failure
        if /ResourceExistsException/.match(failure.message)
          puts("Failed to create secret: #{name}\nAttempting to update it")
          cmdline = "#{put_secret} #{name} --secret-string '#{secret}' --region #{aws_region}"
          run_quiet('Update aws secret', cmdline, system_config)
          puts "Secret updated successfully"
          puts "\n"
        else
          raise failure # re-throw - nothing we can do
        end
      end
    end

    def read_file_content_in(path)
      File.read(path).strip
    end

    def check_file_exists(path)
      File.exist?(path)
    end

    def create_and_store_api_key(api_name, key_name, description, system_config)
      secret = SecureRandom.uuid # secret is a uuid
      hash = Password.create(secret)
      name = "/#{api_name}/#{key_name}"
      store_secret_string(name, hash, description, system_config)
      "Bearer #{Base64.strict_encode64("#{key_name}:#{secret}")}"
    end

    def aae_configuration(system_config, account)
      certificate_dir = File.join(system_config.out, "/certificates/#{account}")

      suffix = Digest::SHA1.hexdigest('COV-8770')[0..4]

      certificate_name = "private-certificate-#{suffix}"
      certificate_path = File.join(certificate_dir, certificate_name + '.csr')

      certificate_key_name = "private-key-#{suffix}"
      certificate_key_path = File.join(certificate_dir, certificate_key_name + '.pem')

      public_key_name = "public-certificate-#{suffix}"
      public_key_path = File.join(certificate_dir, public_key_name + '.cer')

      password_name = "certificate-encryption-password-#{suffix}"
      password_path = File.join(certificate_dir, password_name + '.txt')

      certificate_pkcs12_name = "private-certificate-pkcs12-#{suffix}"
      certificate_pkcs12_path = File.join(certificate_dir, certificate_pkcs12_name + '.p12')

      apim_subscription_name = "subscription-key-#{suffix}"
      apim_subscription_path = File.join(certificate_dir, apim_subscription_name + '.txt')

      password = BCrypt::Password.create(SecureRandom.uuid)

      {
        'apim_subscription_name' => apim_subscription_name,
        'apim_subscription_path' => apim_subscription_path,
        'certificate_dir' => certificate_dir,
        'certificate_name' => certificate_name,
        'certificate_path' => certificate_path,
        'certificate_key_name' => certificate_key_name,
        'certificate_key_path' => certificate_key_path,
        'certificate_pkcs12_name' => certificate_pkcs12_name,
        'certificate_pkcs12_path' => certificate_pkcs12_path,
        'public_key_name' => public_key_name,
        'public_key_path' => public_key_path,
        'password' => password,
        'password_name' => password_name,
        'password_path' => password_path,
        'suffix' => suffix,
        'service_name' => 'aae',
        'consumer_name' => 'advanced_analytics'
      }
    end

    def edge_configuration(system_config)
      token_dir = File.join(system_config.out, 'secrets')
      token_name = 'sas-token'
      token_path = File.join(token_dir, 'edge-sas-token.txt')

      {
        'sas_token_name' => token_name,
        'sas_token' => token_path
      }
    end

    def download_secrets(service_name, consumer_name, aae_config, system_config)
      download_aae_certificate(service_name, consumer_name, aae_config, system_config)
      download_aae_certificate_key(service_name, consumer_name, aae_config, system_config)
      download_aae_certificate_pkcs12(service_name, consumer_name, aae_config, system_config)
      download_aae_public_certificate(service_name, consumer_name, aae_config, system_config)
      download_aae_apim_subscription_key(service_name, consumer_name, aae_config, system_config)
      download_aae_certificate_password(service_name, consumer_name, aae_config, system_config)
    end

    def download_aae_certificate(service_name, consumer_name, aae_config, system_config)
      secret_path = "/#{service_name}/#{consumer_name}"
      certificate_name = aae_config['certificate_name']
      certificate_path = aae_config['certificate_path']
      certificate_dir = aae_config['certificate_dir']
      mkdir_p(certificate_dir, verbose: false)

      cmdline = "#{secret_value} #{secret_path}/#{certificate_name} --query 'SecretString' --output text > #{certificate_path}"
      run_quiet("Download #{secret_path}/#{certificate_name}", cmdline, system_config)
    end

    def download_aae_certificate_key(service_name, consumer_name, aae_config, system_config)
      secret_path = "/#{service_name}/#{consumer_name}"
      certificate_key_name = aae_config['certificate_key_name']
      certificate_key_path = aae_config['certificate_key_path']
      certificate_dir = aae_config['certificate_dir']
      mkdir_p(certificate_dir, verbose: false)

      cmdline = "#{secret_value} #{secret_path}/#{certificate_key_name} --query 'SecretString' --output text > #{certificate_key_path}"
      run_command("Download #{secret_path}/#{certificate_key_name}", cmdline, system_config)
    end

    def download_aae_certificate_pkcs12(service_name, consumer_name, aae_config, system_config)
      secret_path = "/#{service_name}/#{consumer_name}"
      certificate_pkcs12_name = aae_config['certificate_pkcs12_name']
      certificate_pkcs12_path = aae_config['certificate_pkcs12_path']
      certificate_dir = aae_config['certificate_dir']
      mkdir_p(certificate_dir, verbose: false)

      cmdline = "#{secret_value} #{secret_path}/#{certificate_pkcs12_name} --query 'SecretBinary' --output text | base64 -d > #{certificate_pkcs12_path}"
      run_command("Download #{secret_path}/#{certificate_pkcs12_name}", cmdline, system_config)
    end

    def download_aae_public_certificate(service_name, consumer_name, aae_config, system_config)
      secret_path = "/#{service_name}/#{consumer_name}"
      public_key_name = aae_config['public_key_name']
      public_key_path = aae_config['public_key_path']
      certificate_dir = aae_config['certificate_dir']
      mkdir_p(certificate_dir, verbose: false)

      cmdline = "#{secret_value} #{secret_path}/#{public_key_name} --query 'SecretBinary' --output text > #{public_key_path}"
      run_command("Download #{secret_path}/#{public_key_name}", cmdline, system_config)
    end

    def download_aae_certificate_password(service_name, consumer_name, aae_config, system_config)
      secret_path = "/#{service_name}/#{consumer_name}"
      password_name = aae_config['password_name']
      password_path = aae_config['password_path']

      cmdline = "#{secret_value} #{secret_path}/#{password_name} --query 'SecretString' --output text > #{password_path}"
      run_command("Download #{secret_path}/#{password_name}", cmdline, system_config)
    end

    def download_aae_apim_subscription_key(service_name, consumer_name, aae_config, system_config)
      secret_path = "/#{service_name}/#{consumer_name}"
      apim_subscription_name = aae_config['apim_subscription_name']
      apim_subscription_path = aae_config['apim_subscription_path']

      cmdline = "#{secret_value} #{secret_path}/#{apim_subscription_name} --query 'SecretString' --output text > #{apim_subscription_path}"
      run_command("Download #{secret_path}/#{apim_subscription_name}", cmdline, system_config)
    end

    def download_edge_sas_token(system_config, edge_config)
      edge_sas_token_path = edge_config['sas_token']
      cmdline = "#{secret_value} /edge/azure_storage_container/sas-token --query 'SecretString' --output text > #{edge_sas_token_path}"
      run_command('Download /edge/azure_storage_container/sas-token', cmdline, system_config)
    end

    def store_aae_x509_certificates(service_name, consumer_name, aae_config, system_config)
      secret_path = "/#{service_name}/#{consumer_name}"

      certificate_name = aae_config['certificate_name']
      certificate_path = aae_config['certificate_path']
      certificate_key_name = aae_config['certificate_key_name']
      certificate_key_path = aae_config['certificate_key_path']
      public_key_name = aae_config['public_key_name']
      public_key_path = aae_config['public_key_path']
      password_name = aae_config['password_name']
      password = read_file_content_in(aae_config['password_path'])

      puts "Password Value: #{password}"

      store_secret_string("#{secret_path}/#{certificate_name}", "file://#{certificate_path}", 'AAE Mutual TLS Private Certificate (x509)', system_config)
      store_secret_string("#{secret_path}/#{certificate_key_name}", "file://#{certificate_key_path}", 'AAE Mutual TLS Private Encrypted Key', system_config)
      store_secret_string("#{secret_path}/#{password_name}", password, 'AAE Private Key encryption password', system_config)
      store_secret_binary("#{secret_path}/#{public_key_name}", "fileb://#{public_key_path}", 'AAE Mutual TLS Public Certificate', system_config)
    end

    def store_aae_pkcs12_certificates(service_name, consumer_name, aae_config, system_config)
      secret_path = "/#{service_name}/#{consumer_name}"
      certificate_pkcs12_name = aae_config['certificate_pkcs12_name']
      certificate_pkcs12_path = aae_config['certificate_pkcs12_path']

      store_secret_binary("#{secret_path}/#{certificate_pkcs12_name}", "fileb://#{certificate_pkcs12_path}", 'AAE Mutual TLS Private Certificate (PKCS12)', system_config)
    end

    # https://docs.microsoft.com/en-us/azure/api-management/api-management-subscriptions
    def store_aae_apim_subscription_key(service_name, consumer_name, apim_subscription_key, aae_config, system_config)
      secret_path = "/#{service_name}/#{consumer_name}"
      apim_subscription_name = aae_config['apim_subscription_name']

      store_secret_string("#{secret_path}/#{apim_subscription_name}", apim_subscription_key, 'AAE APIM Subscription Key', system_config)
    end

    def store_sas_token(service_name, consumer_name, edge_config, system_config)
      secret_path = "/#{service_name}/#{consumer_name}"
      sas_token_name = edge_config['sas_token_name']
      sas_token = File.read(edge_config['sas_token']).strip
      store_secret_string("#{secret_path}/#{sas_token_name}", sas_token, 'The EDGEs SAS Token', system_config)
    end

    def list_aae_advanced_analytics_secrets(service_name, consumer_name, system_config)
      secret_path = "/#{service_name}/#{consumer_name}"
      cmd = run_command('List aws secrets arn', list_secrets_arn(secret_path), system_config)
      cmd.output
    end
  end
end
