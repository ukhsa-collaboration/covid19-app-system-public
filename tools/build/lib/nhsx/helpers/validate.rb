module NHSx
  # Helpers for facilitating format validation checks in static content
  module Validate
    POLICY_ICONS = ["default-icon", "meeting-people", "bars-and-pubs", "worship", "overnight-stays", "education", "travelling", "exercise", "weddings-and-funerals",
                    "businesses", "retail", "entertainment", "personal-care", "large-events", "clinically-extremly-vulnerable", "social-distancing", "face-coverings",
                    "meeting-outdoors", "meeting-indoors", "work", "international-travel"].freeze
    POLICY_COLOURS = %w[green yellow red amber neutral].freeze
    POLICY_COLOURS_V2 = %w[black maroon green yellow red amber neutral].freeze
    TIERS_WITH_POLICIES = %w[EN.Tier1 EN.Tier2 EN.Tier3 EN.Tier4 WA.Tier1 WA.Tier2 WA.Tier3 WA.Tier4].freeze
    VALID_TIERS = %w[EN.Tier1 EN.Tier2 EN.Tier3 EN.Tier4 EN.Tier4.MassTest EN.Border.Tier1 WA.Tier1 WA.Tier2 WA.Tier3 WA.Tier4
                     EN.HighVHigh EN.MedHigh EN.GenericNeutral EN.MedVHigh EN.NationalRestrictions EN.VariantTier EN.EasingStep1 EN.EasingStep2 EN.VariantTier2].freeze

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
      validation_errors = []
      keys.each do |key|
        valid_key?(metadata, key) & hash?(metadata, key)
        nested_metadata = metadata[key]
        languages.each do |language|
          if nested_metadata[language].nil? || nested_metadata[language].empty?
            validation_errors << "Missing text for language #{language} in #{key}"
          else
            valid_key?(nested_metadata, language) && string?(nested_metadata, language)
          end
        end
        return validation_errors
      end
    end

    def validate_policy_data(metadata, languages)
      valid_key?(metadata, "policyData") & hash?(metadata, "policyData")
      policy_data = metadata["policyData"]
      keys = %w[localAuthorityRiskTitle heading content footer]
      validation_errors = validate_languages(policy_data, keys, languages)
      validation_errors += validate_policies(policy_data, languages)
      return validation_errors
    end

    def validate_policies(policy_data, languages)
      valid_key?(policy_data, "policies") & list?(policy_data, "policies")
      policies = policy_data["policies"]
      validation_errors = []
      policies.each do |policy|
        raise GaudiError, "Policy is not a hash" unless policy.is_a?(Hash)

        valid_key?(policy, "policyIcon") & string?(policy, "policyIcon")
        raise GaudiError, "Invalid policy icon #{policy["policyIcon"]}" unless POLICY_ICONS.include?(policy["policyIcon"])

        language_validation_errors = validate_languages(policy, %w[policyHeading policyContent], languages)
        validation_errors << "Language validation errors for #{policy["policyIcon"]}:\n#{language_validation_errors.join("\n")}" unless language_validation_errors.empty?
      end
      return validation_errors
    end

    def validate_colours(metadata)
      contract_colours_fallback = "neutral"
      valid_key?(metadata, "colorSchemeV2") & string?(metadata, "colorSchemeV2")
      raise GaudiError, "Invalid colorSchemeV2 value #{metadata["colorSchemeV2"]}. Valid values are #{POLICY_COLOURS_V2.join(",")}" unless POLICY_COLOURS_V2.include?(metadata["colorSchemeV2"])

      valid_key?(metadata, "colorScheme") & string?(metadata, "colorScheme")
      raise GaudiError, "Invalid colorScheme value #{metadata["colorScheme"]}. Valid values are #{POLICY_COLOURS.join(",")}" unless POLICY_COLOURS.include?(metadata["colorScheme"])

      raise GaudiError, "Inconsistent colorScheme value #{metadata["colorScheme"]}. Needs to be the same value as colorSchemeV2 #{metadata["colorSchemeV2"]} or #{contract_colours_fallback}" unless colours_match?(metadata, contract_colours_fallback)
    end

    def colours_match?(metadata, fallback)
      (POLICY_COLOURS.include?(metadata["colorSchemeV2"]) && metadata["colorScheme"] == metadata["colorSchemeV2"]) ||
        (!POLICY_COLOURS.include?(metadata["colorSchemeV2"]) && metadata["colorScheme"] == fallback)
    end

    def validate_tier(tier, metadata)
      languages = %w[ar bn cy en gu pa pl ro so tr ur zh]
      validate_colours(metadata)

      validation_errors = validate_languages(metadata, %w[name content linkTitle linkUrl], languages)

      raise GaudiError, "Validation failed for #{tier}:\n#{validation_errors.join("\n")}" unless validation_errors.empty?

      validation_errors += validate_policy_data(metadata, languages) if TIERS_WITH_POLICIES.include? tier
      return validation_errors
    end

    def validate_tiers(tier_metadata)
      unexpected_tiers = tier_metadata.keys - VALID_TIERS
      raise GaudiError, "Tier metadata contains unexpected tiers: #{unexpected_tiers.join(",")}" unless unexpected_tiers.empty?

      validation_failed = false
      VALID_TIERS.each do |tier|
        begin
          puts "Validating #{tier}"

          valid_key?(tier_metadata, tier) & hash?(tier_metadata, tier)
          metadata = tier_metadata[tier]
          validation_errors = validate_tier(tier, metadata)
          raise GaudiError, "Validation failed for #{tier}:\n#{validation_errors.join("\n")}" unless validation_errors.empty?
        rescue GaudiError => e
          puts e.message
          validation_failed = true
        end
      end
      raise GaudiError, "Tier metadata validation failed" if validation_failed
    end
  end
end
