namespace :i18n do
  desc "Update the system resources with translations from Localise"
  task :update do
    include NHS::I18N

    translations = load_translations($configuration.localise_input)
    update_questionaire(translations, $configuration)
    update_android_availability_configuration(translations, $configuration)
    update_ios_availability_configuration(translations, $configuration)
    update_tier_metadata(translations, $configuration)
  end
end
