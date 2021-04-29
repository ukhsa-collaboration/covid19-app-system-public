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
    tiers = %w[EN.Tier1 EN.Tier2 EN.Tier3 EN.Tier4 EN.Tier4.MassTest EN.Border.Tier1 WA.Tier1 WA.Tier2 WA.Tier3 WA.Tier4 EN.HighVHigh EN.MedHigh EN.GenericNeutral EN.MedVHigh EN.NationalRestrictions EN.VariantTier EN.EasingStep1 EN.EasingStep2 EN.VariantTier2]
    unexpected_tiers = file_content.keys - tiers
    raise GaudiError, "Tier metadata contains unexpected tiers: #{unexpected_tiers.join(",")}" unless unexpected_tiers.empty?

    validation_errors = false
    tiers.each do |tier|
      begin
        puts "Validating #{tier}"

        valid_key?(file_content, tier) & hash?(file_content, tier)
        metadata = file_content[tier]
        validate_tier(tier, metadata)
      rescue GaudiError => e
        puts e.message
        validation_errors = true
      end
    end
    raise GaudiError, "Tier metadata validation failed" if validation_errors
  end
end
