def create_linked_secrets(api_name, auth_key_name, hash_key_name)
  authorization_header = create_and_store_api_key(api_name, hash_key_name)
  name = "/#{api_name}/#{auth_key_name}"
  store_secret(authorization_header, name, "eu-west-1") # cf. src/synthetics/accounts/staging/terraform.tf
end

namespace :synth do
  namespace :secret do
    include Zuehlke::Execution
    include NHSx::Secret
    include BCrypt

    hash_key_name = "synthetic_canary"
    auth_key_name = "#{hash_key_name}_auth"
    NHSx::TargetEnvironment::API_NAMES.each do |api_name, rake_task|
      NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.keys.each do |account|
        prerequisites = [:"login:#{account}"]
        desc "Create bearer token and secret password hash for #{api_name} synthetic canaries in the #{account} account"
        task :"#{rake_task}:#{account}" => prerequisites do
          create_linked_secrets(api_name, auth_key_name, hash_key_name)
        end
      end
    end
  end
end
