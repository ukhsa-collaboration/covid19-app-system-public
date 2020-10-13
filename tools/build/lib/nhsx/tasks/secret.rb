require "securerandom"
require "base64"
require "bcrypt"

namespace :secret do
  include Zuehlke::Execution
  include NHSx::Secret
  include BCrypt

  NHSx::TargetEnvironment::API_NAMES.each do |api_name, rake_task|
    NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      prerequisites = [:"login:#{account}"]
      desc "Create bearer token and store secret password hash for #{api_name} APIs with API_KEY_NAME"
      task :"#{rake_task}:#{account}" => prerequisites do
        key_name = $configuration.api_key_name # check value in AWS secrets manager
        authorization_header = create_and_store_api_key(api_name, key_name)
        puts "*" * 74
        puts "Clients need to add the following http header to their requests:"
        puts "Authorization: #{authorization_header}"
      end
    end
  end
end

namespace :certificate do
  desc "Updates an existing client certificate in Secrets Manager on dev"
  task :"update:dev" do
    include NHSx::Secret
    service_name = "aae"
    consumer_name = "advanced_analytics"
    new_certificate_config = create_aae_certificate($configuration)
    secret_manager_arns = update_aae_certificate_config(service_name, consumer_name, new_certificate_config, $configuration)
    print_aae_certificate_issuer(new_certificate_config)
    print_aae_certificate_fingerprint(new_certificate_config)
    clean_aae_certificate(new_certificate_config)
    puts "*" * 74
    puts "AWS Account User needs to share the public key with AAE Environment"
    puts new_certificate_config["public_key_name"]
    puts "*" * 74
    puts secret_manager_arns
  end
  desc "Updates an existing client certificate in Secrets Manager on prod"
  task :"update:prod" => [:"login:prod"] do
    include NHSx::Secret
    service_name = "aae"
    consumer_name = "advanced_analytics"
    new_certificate_config = create_aae_certificate($configuration)
    secret_manager_arns = update_aae_certificate_config(service_name, consumer_name, new_certificate_config, $configuration)
    print_aae_certificate_issuer(new_certificate_config)
    print_aae_certificate_fingerprint(new_certificate_config)
    clean_aae_certificate(new_certificate_config)
    puts "*" * 74
    puts "AWS Account User needs to share the public key with AAE Environment"
    puts new_certificate_config["public_key_name"]
    puts "*" * 74
    puts secret_manager_arns
  end
end
