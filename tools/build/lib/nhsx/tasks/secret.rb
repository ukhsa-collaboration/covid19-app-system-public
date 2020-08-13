require "securerandom"
require "base64"
require "bcrypt"

namespace :secret do
  include Zuehlke::Execution
  include NHSx::Secret
  include BCrypt

  desc "Creates an api key for mobile apis with API_KEY_NAME"
  task :mobile do
    api_name = "mobile" # see uk.nhs.nhsx.core.auth.ApiName
    key_name = $configuration.api_key_name
    authorization_header = create_and_store_api_key(api_name, key_name)
    puts "*" * 74
    puts "Clients need to add the following http header to their requests:"
    puts "Authorization: #{authorization_header}"
  end
  desc "Creates an api key for test result apis with API_KEY_NAME"
  task :test_result do
    api_name = "testResultUpload" # see uk.nhs.nhsx.core.auth.ApiName
    key_name = $configuration.api_key_name
    authorization_header = create_and_store_api_key(api_name, key_name)
    puts "*" * 74
    puts "Clients need to add the following http header to their requests:"
    puts "Authorization: #{authorization_header}"
  end
  desc "Creates an api key for high risk venue apis with API_KEY_NAME"
  task :venues do
    api_name = "highRiskVenuesCodeUpload" # see uk.nhs.nhsx.core.auth.ApiName
    key_name = $configuration.api_key_name
    authorization_header = create_and_store_api_key(api_name, key_name)
    puts "*" * 74
    puts "Clients need to add the following http header to their requests:"
    puts "Authorization: #{authorization_header}"
  end
  desc "Creates an api key for high risk post district apis with API_KEY_NAME"
  task :post_districts do
    api_name = "highRiskPostCodeUpload" # see uk.nhs.nhsx.core.auth.ApiName
    key_name = $configuration.api_key_name # check value in aws secrets manager
    authorization_header = create_and_store_api_key(api_name, key_name)
    puts "*" * 74
    puts "Clients need to add the following http header to their requests:"
    puts "Authorization: #{authorization_header}"
  end
end
