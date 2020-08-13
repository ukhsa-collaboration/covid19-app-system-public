module NHSx
  # Helper methods for creating api keys
  module Secret
    def create_and_store_api_key(api_name, key_name)
      name = "/#{api_name}/#{key_name}"

      secret = SecureRandom.uuid # secret is a uuid
      hash = Password.create(secret)
      authorization_header = "Bearer #{Base64.strict_encode64("#{key_name}:#{secret}")}"

      # Warning: a secret can only be deleted after a minimum period of 7 days
      # This script fails if the secret name already exists
      cmdline = "aws secretsmanager create-secret --name #{name} --secret-string '#{hash}' --region #{NHSx::AWS::AWS_REGION}"
      run_command("Create aws secret", cmdline, $configuration)
      return authorization_header
    end
  end
end
