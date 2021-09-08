require "securerandom"
require "base64"
require "bcrypt"

namespace :secret do
  include Zuehlke::Execution
  include NHSx::Secret
  include BCrypt

  NHSx::TargetEnvironment::API_NAMES.each do |api_name, rake_task|
    NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, _tgt_envs|
      prerequisites = [:"login:#{account}"]
      desc "Create bearer token and store secret password hash for #{api_name} APIs with API_KEY_NAME"
      task :"#{rake_task}:#{account}" => prerequisites do
        key_name = $configuration.api_key_name # check value in AWS secrets manager
        authorization_header = create_and_store_api_key(api_name, key_name, "CTA API Auth bearer token", $configuration)
        puts "*" * 74
        puts "Clients need to add the following http header to their requests:"
        puts "Authorization: #{authorization_header}"
      end
    end
  end
end
