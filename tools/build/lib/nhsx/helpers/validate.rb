module NHSx
  # Helpers for facilitating format validation checks in static content
  module Validate
    # Validate the given key is present in file_content, raise GaudiError otherwise
    def valid_key?(file_content, key)
      raise GaudiError, "Mandatory key #{key} not present" unless file_content.key?(key)
    end

    # Validate the given key is hash, raise GaudiError otherwise
    def hash?(file_content, key)
      raise GaudiError, "Key #{key} is not a hash" unless file_content[key].is_a?(Hash)
    end

    # Validate the given key is Integer, raise GaudiError otherwise
    def integer?(file_content, key)
      raise GaudiError, "Key #{key} is not an Integer" unless file_content[key].is_a?(Integer)
    end

    # Validate the given key is String, raise GaudiError otherwise
    def string?(file_content, key)
      raise GaudiError, "Key #{key} is not an String" unless file_content[key].is_a?(String)
    end

    def list?(file_content, key)
      raise GaudiError, "Key #{key} is not an String" unless file_content[key].is_a?(Array)
    end

    def validate_languages(metadata, keys, languages)
      keys.each do |key|
        valid_key?(metadata, key) & hash?(metadata, key)
        nested_metadata = metadata[key]
        languages.each do |language|
          if nested_metadata[language].nil?
            puts "Missing text for language #{language} in #{key}"
          else
            valid_key?(nested_metadata, language) && string?(nested_metadata, language)
          end
        end
      end
    end

    def validate_policy_data(metadata, languages)
      valid_key?(metadata, "policyData") & hash?(metadata, "policyData")
      policy_data = metadata["policyData"]
      keys = %w[localAuthorityRiskTitle heading content footer]
      validate_languages(policy_data, keys, languages)
      validate_policies(policy_data, languages)
    end

    def validate_policies(policy_data, languages)
      valid_key?(policy_data, "policies") & list?(policy_data, "policies")
      policies = policy_data["policies"]
      policies.each do |policy|
        raise GaudiError, "Policy is not a hash" unless policy.is_a?(Hash)
        valid_key?(policy, "policyIcon") & string?(policy, "policyIcon")
        validate_languages(policy, %w[policyHeading policyContent], languages)
      end
    end
  end
end
