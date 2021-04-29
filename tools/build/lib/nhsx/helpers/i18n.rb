require "json"

module NHS
  # Methods to facilitate working with i18n data and tools
  module I18N
    # Path to the symptomatic questionaire configuration file
    QUESTIONAIRE = "src/static/symptomatic-questionnaire.json".freeze
    # Path to the android availability configuration file
    AVAILABILITY_ANDROID = "src/static/availability-android.json".freeze
    # Path to the the ios availability configuration file
    AVAILABILITY_IOS = "src/static/availability-ios.json".freeze
    # Path to the post district risk level configuration file
    TIERS = "src/static/tier-metadata.json".freeze
    # Policy icons mapping i18n keys to mobile icon keys
    POLICY_ICONS = {
      "Bars" => "bars-and-pubs",
      "Education" => "education",
      "Exercise" => "exercise",
      "FaceCoverings" => "face-coverings",
      "InternationalTravel" => "international-travel",
      "MeetingIndoors" => "meeting-indoors",
      "MeetingOutdoors" => "meeting-outdoors",
      "MeetingPeople" => "meeting-people",
      "Overnight" => "overnight-stays",
      "PersonalCare" => "personal-care",
      "Retail" => "retail",
      "SocialDistancing" => "social-distancing",
      "Travelling" => "travelling",
      "WeddingFuneral" => "weddings-and-funerals",
      "Work" => "work",
      "Worship" => "worship"

    }
    # Number of supported languages ex: en, ar, bn, cy
    SUPPORTED_LANGUAGES = ["ar","bn","cy","en","gu","pa","pl","ro","so","tr","ur","zh"]
    # Loads the tranlsation data from an export of Localise
    #
    # Localise exports data in a "locale" directory, one file per language.
    #
    # To get the complete set of i18n keys export everything in Localise that has the tag 'backend'
    def load_translations(localise_input)
      raise GaudiError, "Missing Localise input directory '#{localise_input}'" unless File.exist?(localise_input)

      i18n_files = Rake::FileList["#{localise_input}/*.json"]
      puts "Found localisation data for #{i18n_files.size} language(s)"

      translations = {}
      i18n_files.each do |i18n_file|
        localise_data = JSON.parse(File.read(i18n_file)).sort_by { |key| key }.to_h
        localise_data.each do |k, v|
          translations[k] ||= {}
          translations[k][i18n_file.pathmap("%n")] = v["translation"]
        end
      end
      return translations
    end

    # Updates the translation texts in the symptomatic questionaire configuration file
    def update_questionaire(translations, system_config)
      questionaire_file = File.join(system_config.base, QUESTIONAIRE)
      symptomatic_questionaire = JSON.parse(File.read(questionaire_file))

      symptomatic_questionaire["symptoms"].each_with_index do |_, idx|
        symptomatic_questionaire["symptoms"][idx]["title"] = translations["question#{idx + 1}.title"]
        symptomatic_questionaire["symptoms"][idx]["description"] = translations["question#{idx + 1}.description"]
      end
      write_file(questionaire_file, JSON.pretty_generate(symptomatic_questionaire))
    end

    def update_tier_metadata(translations, system_config)
      tier_metadata_file = File.join(system_config.base, TIERS)

      tier_metadata = JSON.parse(File.read(tier_metadata_file))

      tier_metadata.keys.each do |tier_label|
        tier_metadata[tier_label]["name"] = translations["#{tier_label}.Name"]
        tier_metadata[tier_label]["heading"] = translations["#{tier_label}.Heading"]
        tier_metadata[tier_label]["content"] = translations["#{tier_label}.Content"]
        tier_metadata[tier_label]["linkUrl"] = translations["#{tier_label}.LinkURL"]
        tier_metadata[tier_label]["linkTitle"] = translations["#{tier_label}.LinkTitle"]

        tier_policies = {}
        translations.keys.select { |v| v.start_with?("#{tier_label}.PolicyData.Policies.") }
          .map { |v| v.gsub("#{tier_label}.PolicyData.Policies.", "").split(".").first }.each do |policy_icon|
          raise GaudiError, "I18n key #{policy_icon} does not map to an icon" unless POLICY_ICONS.keys.include?(policy_icon)

          tier_policies[policy_icon] = {
            "policyIcon" => POLICY_ICONS[policy_icon],
            "policyHeading" => translations["#{tier_label}.PolicyData.Policies.#{policy_icon}.Heading"],
            "policyContent" => translations["#{tier_label}.PolicyData.Policies.#{policy_icon}.Content"],
          }
        end

        if tier_policies.empty?
          puts "Warning: #{tier_label} has no policy data"
        else
          tier_policies.each do |k, v|
            v.each do |k1, v1|
              if v1.class == Hash
                v1.transform_values! { |entry| entry.empty? ? nil : entry }
                puts "Warning: missing translations for #{tier_label}.PolicyData.Policies.#{k} => #{k1}" if v1.compact.size != v1.size
                v1 = v1.compact!
              end
            end

            if v["policyHeading"].empty? || v["policyContent"].empty?
              tier_policies.delete(k)
            elsif v["policyHeading"].keys != SUPPORTED_LANGUAGES || v["policyContent"].keys != SUPPORTED_LANGUAGES
              if v["policyHeading"].keys != SUPPORTED_LANGUAGES
                puts "Error: policy #{k} does not have the translations for the following locales: #{SUPPORTED_LANGUAGES-v["policyHeading"].keys} in policy heading, include them in Lokalise?"
              end
              if v["policyContent"].keys != SUPPORTED_LANGUAGES
                puts "Error: policy #{k} does not have the translations for the following locales:  #{SUPPORTED_LANGUAGES-v["policyContent"].keys} in policy content, include them in Lokalise?"
              end
            end

          end

          tier_metadata[tier_label]["policyData"] = {
            "localAuthorityRiskTitle" => translations["#{tier_label}.PolicyData.localAuthorityRiskTitle"],
            "heading" => translations["#{tier_label}.PolicyData.Heading"],
            "content" => translations["#{tier_label}.PolicyData.Content"],
            "footer" => translations["#{tier_label}.PolicyData.Footer"],
            "policies" => tier_policies.map { |_, v| v },
          }
        end

        puts "Warning: #{tier_label} has no translations" unless translations["#{tier_label}.Name"]
      end

      write_file(tier_metadata_file, JSON.pretty_generate(tier_metadata))
    end

    # Updates the translation texts in the android availability configuration file
    def update_android_availability_configuration(translations, system_config)
      android_availability_file = File.join(system_config.base, AVAILABILITY_ANDROID)

      android_availability = JSON.parse(File.read(android_availability_file))
      android_availability["minimumSDKVersion"]["description"] = translations["android.minimumSDKVersion.description"]
      android_availability["minimumAppVersion"]["description"] = translations["android.minimumAppVersion.description"]
      android_availability["recommendedAppVersion"]["description"] = translations["android.RecommendedAppVersion.description"]
      android_availability["recommendedAppVersion"]["title"] = translations["android.RecommendedAppVersion.Title"]

      write_file(android_availability_file, JSON.pretty_generate(android_availability))
    end

    # Updates the translation texts in the ios availability configuration file
    def update_ios_availability_configuration(translations, system_config)
      ios_availability_file = File.join(system_config.base, AVAILABILITY_IOS)

      ios_availability = JSON.parse(File.read(ios_availability_file))
      ios_availability["minimumOSVersion"]["description"] = translations["ios.minimumOSVersion.description"]
      ios_availability["minimumAppVersion"]["description"] = translations["ios.minimumAppVersion.description"]
      ios_availability["recommendedOSVersion"]["title"] = translations["ios.RecommendedOSVersion.Title"]
      ios_availability["recommendedOSVersion"]["description"] = translations["ios.RecommendedOSVersion.description"]
      ios_availability["recommendedAppVersion"]["title"] = translations["ios.RecommendedAppVersion.Title"]
      ios_availability["recommendedAppVersion"]["description"] = translations["ios.RecommendedAppVersion.description"]

      write_file(ios_availability_file, JSON.pretty_generate(ios_availability))
    end
  end
end
