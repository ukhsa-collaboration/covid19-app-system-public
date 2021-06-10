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
    voc_payload = construct_local_messages_payload(local_messages_metadata)

    json_payload = File.join($configuration.base, "src/static/local-messages-metadata.json")
    write_file(json_payload, JSON.pretty_generate(voc_payload))
  end
end
