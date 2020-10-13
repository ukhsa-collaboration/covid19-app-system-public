require "shellwords"
require "bcrypt"

module NHSx
  # Helper methods for creating api keys
  module Secret
    include Zuehlke::Execution

    def store_secret(secret_string, name, aws_region)
      # Warning: a secret can only be deleted after a minimum period of 7 days
      # This script may fail if the secret name already exists but is pending deletion
      cmdline = "aws secretsmanager create-secret --name #{name} --secret-string '#{secret_string}' --region #{aws_region}"
      begin
        run_command("Create aws secret", cmdline, $configuration)
      rescue GaudiError => failure
        if /ResourceExistsException/.match(failure.message)
          puts("Failed to create the secret - trying to update it instead")
          cmdline = "aws secretsmanager update-secret --secret-id #{name} --secret-string '#{secret_string}' --region #{aws_region}"
          run_command("Update aws secret", cmdline, $configuration)
        else
          raise failure # re-throw - nothing we can do
        end
      end
    end

    def create_and_store_api_key(api_name, key_name)
      secret = SecureRandom.uuid # secret is a uuid
      hash = Password.create(secret)
      name = "/#{api_name}/#{key_name}"
      store_secret(hash, name, NHSx::AWS::AWS_REGION)
      authorization_header = "Bearer #{Base64.strict_encode64("#{key_name}:#{secret}")}"
      return authorization_header
    end

    def create_aae_certificate(system_config)
      certificate_dir = File.join(system_config.out, "/certificates")
      rm_rf(certificate_dir, :verbose => false)
      mkdir_p(certificate_dir, :verbose => false)

      certificate_name = File.join(certificate_dir, "delete-this-cert.csr")
      certificate_key_name = File.join(certificate_dir, "delete-this-key.pem")

      public_key_name = File.join(certificate_dir, "delete-this-public-key.cer")
      subject = "'/C=GB/ST=London/L=London/O=NHS/OU=Test and Trace/CN=svc-test-trace.nhs.uk/emailAddress=one.email@nhsx.nhs.uk'"

      pass = BCrypt::Password.create(SecureRandom.uuid)
      escaped_pass = Shellwords.escape(pass)

      cmdline = "openssl req -x509 -newkey rsa:4096 -days 365 -outform PEM -keyout #{certificate_key_name} -out #{certificate_name} -passin pass:#{escaped_pass} -passout pass:#{escaped_pass} -subj #{subject}"
      run_quiet("Create aws secret", cmdline, $configuration)

      cmdline = "openssl x509 -outform der -in #{certificate_name} -out #{public_key_name}"
      run_command("Create public certificate", cmdline, $configuration)

      certificate_config = {
        "certificate_name" => certificate_name,
        "certificate_key_name" => certificate_key_name,
        "public_key_name" => public_key_name,
        "password" => escaped_pass,
      }

      return certificate_config
    end

    def update_aae_certificate_config(service_name, consumer_name, certificate_config, system_config)
      secret_path = "/#{service_name}/#{consumer_name}"
      suffix = Digest::SHA1.hexdigest(secret_path)[0..4]

      certificate_name = certificate_config["certificate_name"]
      certificate_key_name = certificate_config["certificate_key_name"]
      password = certificate_config["password"]

      awscmd = "aws secretsmanager update-secret --region #{NHSx::AWS::AWS_REGION} --secret-id"

      cmdline = "#{awscmd} #{secret_path}/private-certificate-#{suffix} --secret-string file://#{certificate_name} --description 'AAE Mutual TLS Private Certificate (x509)'"
      run_quiet("Update aws secret", cmdline, system_config)

      cmdline = "#{awscmd} #{secret_path}/private-key-#{suffix} --secret-string file://#{certificate_key_name} --description 'AAE Mutual TLS Private Encrypted Key'"
      run_quiet("Update aws secret", cmdline, system_config)

      cmdline = "#{awscmd} #{secret_path}/certificate-encryption-password-#{suffix} --secret-string #{password} --description 'AAE Private Key encryption password'"
      run_quiet("Update aws secret", cmdline, system_config)

      cmdline = "aws secretsmanager list-secrets --region #{NHSx::AWS::AWS_REGION} --filters Key=name,Values=[\"/aae/advanced_analytics/\"] --query 'SecretList[*].ARN' --output json"
      cmd = run_command("List aws secrets arn", cmdline, system_config)
      return cmd.output
    end

    def print_aae_certificate_issuer(certificate_config)
      cmdline = "openssl x509 -in #{certificate_config["certificate_name"]} -issuer -noout"
      cmd = run_command("Print the certificate's issuer", cmdline, system_config)
      return cmd.output
    end

    def print_aae_certificate_fingerprint(certificate_config)
      cmdline = "openssl x509 -in #{certificate_config["certificate_name"]} -fingerprint -noout"
      cmd = run_command("Print the certificate's fingerprint", cmdline, system_config)
      return cmd.output
    end

    def clean_aae_certificate(certificate_config)
      rm_rf(certificate_config["certificate_name"], :verbose => false)
      rm_rf(certificate_config["certificate_key_name"], :verbose => false)
    end
  end
end
