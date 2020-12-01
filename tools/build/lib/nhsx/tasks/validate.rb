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
    tiers = %w[EN.Tier1 EN.Tier2 EN.Tier3 EN.Tier4 EN.Tier4.MassTest EN.Border.Tier1 WA.Tier1 WA.Tier2 WA.Tier3 EN.HighVHigh EN.MedHigh EN.GenericNeutral EN.MedVHigh]
    languages = %w[ar bn cy en gu pa pl ro so tr ur zh]
    tiers.each do |tier|
      puts "Validating #{tier}"
      valid_key?(file_content, tier) & hash?(file_content, tier)

      metadata = file_content[tier]
      valid_key?(metadata, "colorScheme") & string?(metadata, "colorScheme")
      validate_languages(metadata, %w[name heading content linkTitle linkUrl], languages)

      validate_policy_data(metadata, languages) if %w[EN.Tier1 EN.Tier2 EN.Tier3].include? tier
    end
  end
end
