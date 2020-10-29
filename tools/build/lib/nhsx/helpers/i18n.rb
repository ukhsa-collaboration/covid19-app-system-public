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
        localise_data = JSON.parse(File.read(i18n_file))
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
        puts "Warning #{tier_label} has no translations" unless translations["#{tier_label}.Name"]
      end

      write_file(tier_metadata_file, JSON.pretty_generate(tier_metadata))
    end

    # Updates the translation texts in the android availability configuration file
    def update_android_availability_configuration(translations, system_config)
      android_availability_file = File.join(system_config.base, AVAILABILITY_ANDROID)

      android_availability = JSON.parse(File.read(android_availability_file))
      android_availability["minimumSDKVersion"]["description"] = translations["android.minimumSDKVersion.description"]
      android_availability["minimumAppVersion"]["description"] = translations["android.minimumAppVersion.description"]

      write_file(android_availability_file, JSON.pretty_generate(android_availability))
    end

    # Updates the translation texts in the ios availability configuration file
    def update_ios_availability_configuration(translations, system_config)
      ios_availability_file = File.join(system_config.base, AVAILABILITY_IOS)

      ios_availability = JSON.parse(File.read(ios_availability_file))
      ios_availability["minimumOSVersion"]["description"] = translations["ios.minimumOSVersion.description"]
      ios_availability["minimumAppVersion"]["description"] = translations["ios.minimumAppVersion.description"]

      write_file(ios_availability_file, JSON.pretty_generate(ios_availability))
    end
  end
end
