namespace :import do
  desc "Download content from Lokalise"
  task :local_messages => [:"login:dev"] do
    include NHSx::AWS
    include NHS::Import
    include NHSx::Validate
    include Gaudi::Utilities

    secret = secrets_entry("/lokalise/apiKey", $configuration)
    api_key = JSON.parse(secret)["apiKey"]
    project_id = "4117786960a291eb624dc6.04622656" # project id for App Messaging
    local_messages_metadata = import_local_messages(api_key, project_id, $configuration)
    validate_local_messages_metadata_languages(local_messages_metadata)
    la_message_mapping = JSON.parse(File.read(File.join($configuration.base, "src/static/local-authority-message-mapping.json")))
    validate_local_messages(la_message_mapping["las"], local_messages_metadata)
    voc_message_metadata = construct_local_messages_metadata(local_messages_metadata)

    warnings = validate_local_messages_placeholders(voc_message_metadata)
    puts "Finished with some warnings: " unless warnings.empty?
    warnings.each { |k, v| puts "#{k} => #{v}" }

    json_message_metadata = File.join($configuration.base, "src/static/local-messages-metadata.json")
    write_file(json_message_metadata, JSON.pretty_generate(voc_message_metadata))
  end
  desc "Download the i18n content from Lokalise and add it to the system"
  task :translations => [:"login:dev"] do
    include NHS::Import
    include NHS::I18N

    secret = secrets_entry("/lokalise/apiKey", $configuration)
    api_key = JSON.parse(secret)["apiKey"]
    project_id = "895873615f401231224445.23171698" # project id for NHS COVID19

    translations = import_translations(api_key, project_id, $configuration)

    update_questionaire(translations, $configuration)
    update_android_availability_configuration(translations, $configuration)
    update_ios_availability_configuration(translations, $configuration)
    update_tier_metadata(translations, $configuration)
  end
end
