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
    tier_metadata = JSON.parse(File.read(config_file_location))
    validate_tiers(tier_metadata)
  end
  desc "Validate local messages"
  task :local_messages do
    include NHSx::Validate
    metadata_file = File.join($configuration.base, "src/static/local-messages-metadata.json")
    mapping_file = $configuration.message_mapping($configuration)
    local_messages_metadata = JSON.parse(File.read(metadata_file))
    la_message_mapping = JSON.parse(File.read(mapping_file))
    puts "Validating \n Mapping file #{mapping_file}\n Message metadata: #{metadata_file}"
    validate_local_messages(la_message_mapping["las"], local_messages_metadata["messages"])
  end

  desc "Validate analytics fields"
  task :"analytics-fields" do
    include NHSx::Validate

    class DuplicateCheckingHash < Hash
      attr_accessor :duplicate_check_off

      def []=(key, value)
        raise GaudiError, "Failed: Found duplicate key \"#{key}\" while parsing json! Please cleanup your JSON input!" if !duplicate_check_off && has_key?(key)
        super
      end
    end

    fields_file_location = File.join($configuration.base, "/src/aws/analytics_fields/fields.json")
    analytics_fields = JSON.parse(File.read(fields_file_location), { :object_class => DuplicateCheckingHash })
    validate_fields(analytics_fields["mobile_analytics"])
    validate_fields(
      analytics_fields["mobile_events_analytics"],
      additional_allowed_types: ["double", "array<struct<secondsSinceLastScan:int,minimumAttenuation:int,typicalAttenuation:int>>"]
    )
  end
end
