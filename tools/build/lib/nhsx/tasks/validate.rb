namespace :validate do
  desc "Validate all static content"
  task :static => [:"validate:en-config"]
  desc "Validate exposure configuration static content"
  task :"en-config" do
    include NHSx::Validate
    config_file_location = File.join($configuration.base, "/src/static/exposure-configuration.json")
    file_content = JSON.parse(File.read(config_file_location))

    puts "Validating #{config_file_location}"
    valid_key?(file_content, "exposureNotification") & hash?(file_content, "exposureNotification")
    valid_key?(file_content, "riskCalculation") & hash?(file_content, "riskCalculation")
    valid_key?(file_content, "riskScore") & hash?(file_content, "riskScore")
    valid_key?(file_content, "v2RiskCalculation") & hash?(file_content, "v2RiskCalculation")

    risk_calculation = file_content["riskCalculation"]
    risk_calculation_v2 = file_content["v2RiskCalculation"]
    valid_key?(risk_calculation, "riskThreshold") & integer?(risk_calculation, "riskThreshold")
    valid_key?(risk_calculation_v2, "riskThreshold") & integer?(risk_calculation_v2, "riskThreshold")
    puts "Validation successful"
  end

  desc "Validate tier metadata"
  task :"tier-metadata" do
    include NHSx::Validate
    config_file_location = File.join($configuration.base, "/src/static/tier-metadata.json")
    file_content = JSON.parse(File.read(config_file_location))
    tiers = %w[EN.Tier1 EN.Tier2 EN.Tier3 EN.Tier4 EN.Tier4.MassTest EN.Border.Tier1 WA.Tier1 WA.Tier2 WA.Tier3 WA.Tier4 EN.HighVHigh EN.MedHigh EN.GenericNeutral EN.MedVHigh EN.NationalRestrictions EN.VariantTier]
    unexpected_tiers = file_content.keys - tiers
    raise GaudiError, "Tier metadata contains unexpected tiers: #{unexpected_tiers.join(",")}" unless unexpected_tiers.empty?

    languages = %w[ar bn cy en gu pa pl ro so tr ur zh]
    tiers.each do |tier|
      puts "Validating #{tier}"
      valid_key?(file_content, tier) & hash?(file_content, tier)
      metadata = file_content[tier]

      contract_colours = ["green", "yellow", "red", "amber", "neutral"]
      contract_colours_fallback = "neutral"
      contract_colours_v2 = ["black", "maroon", "green", "yellow", "red", "amber", "neutral"]
      icons = ["default-icon", "meeting-people", "bars-and-pubs", "worship", "overnight-stays", "education", "travelling", "exercise", "weddings-and-funerals", "businesses", "retail", "entertainment", "personal-care", "large-events", "clinically-extremly-vulnerable", "social-distancing", "face-coverings", "meeting-outdoors", "meeting-indoors", "work", "international-travel"]

      valid_key?(metadata, "colorSchemeV2") & string?(metadata, "colorSchemeV2")
      raise GaudiError, "Invalid colorSchemeV2 value #{metadata["colorSchemeV2"]}. Valid values are #{contract_colours_v2.join(",")}" unless contract_colours_v2.include?(metadata["colorSchemeV2"])

      valid_key?(metadata, "colorScheme") & string?(metadata, "colorScheme")
      raise GaudiError, "Invalid colorScheme value #{metadata["colorScheme"]}. Valid values are #{contract_colours.join(",")}" unless contract_colours.include?(metadata["colorScheme"])

      if contract_colours.include?(metadata["colorSchemeV2"]) && metadata["colorScheme"] != metadata["colorSchemeV2"]
        raise GaudiError, "Inconsistent colorScheme value #{metadata["colorScheme"]}. Needs to be the same value as colorSchemeV2 #{metadata["colorSchemeV2"]}"
      end

      if !contract_colours.include?(metadata["colorSchemeV2"]) && metadata["colorScheme"] != contract_colours_fallback
        raise GaudiError, "Inconsistent colorScheme value #{metadata["colorScheme"]}. For colorSchemeV2 values not supported in colorScheme the fallback #{contract_colours_fallback} should be used."
      end

      validation_errors = validate_languages(metadata, %w[name content linkTitle linkUrl], languages)

      raise GaudiError, "Validation failed for #{tier}:\n#{validation_errors.join("\n")}" unless validation_errors.empty?

      validate_policy_data(metadata, languages) if %w[EN.Tier1 EN.Tier2 EN.Tier3].include? tier
    end
  end
end
