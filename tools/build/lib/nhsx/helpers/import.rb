require "nokogiri"
require "ruby-lokalise-api"

module NHS
  module Import
    # Imports all entries in the NHS COVID19 Localise project that are tagged with backend
    def import_translations(api_key, project_id, system_config)
      lokalise_client = Lokalise.client api_key

      lokalise_messages = retrieve_all_lokalise_keys(lokalise_client, project_id, "backend", 500)
      lokalise_export = {}
      lokalise_messages.each do |key|
        key_name = key.key_name["web"]
        translations = lokalise_client.key(project_id, key.key_id, {}).translations
        translations.each do |translated_key|
          lang_iso = translated_key["language_iso"].split("_").first
          translation_text = translated_key["translation"]
          lokalise_export[key_name] ||= {}
          lokalise_export[key_name][lang_iso] = translation_text
        end
      end
      lokalise_export.each do |field_key, translations|
        lokalise_export[field_key] = translations.sort_by { |lang_key, _| lang_key }.to_h
      end
      return lokalise_export
    end

    def import_local_messages(api_key, project_id, system_config)
      lokalise_client = Lokalise.client api_key

      # find matching key_id for key_name
      voc_messages = {}
      error_messages = []

      lokalise_messages = retrieve_all_lokalise_keys(lokalise_client, project_id, "message", 500)
      lokalise_messages.each do |key|
        message_key = key.key_name["web"]
        # download and check the schema
        valid_translations, validation_errors = extract_local_message_translations(api_key, key, project_id, system_config)
        error_messages += validation_errors
        voc_messages[message_key] = valid_translations
      end
      raise GaudiError, "schema validation error\n#{error_messages.join("\n")}" unless error_messages.empty?

      return voc_messages
    end

    def retrieve_all_lokalise_keys(lokalise_client, project_id, filter_tag, limit)
      lokalise_all_keys = []
        for i in 1..lokalise_client.keys(project_id, { :limit => limit }).total_pages
          lokalise_all_keys.concat(lokalise_client.keys(project_id, { :limit => limit, :page => i }).collection)
        end
      return lokalise_all_keys.select { |e| e.tags.include? filter_tag }
    end

    def extract_local_message_translations(api_key, lokalise_key, lokalise_project, system_config)
      valid_translations = {}
      missing_translations = []
      error_messages = []
      schema = Nokogiri::XML::Schema(File.read(File.join(system_config.base, "tools/lokalise/message.xsd")))
      lokalise_client = Lokalise.client api_key

      lokalise_translations = lokalise_client.key(lokalise_project, lokalise_key.key_id, {}).translations

      lokalise_translations.each do |k|
        lang_iso = k["language_iso"].split("_").first
        key_name = lokalise_key.key_name["web"]
        if k["translation"].empty?
          missing_translations << "Missing translation for #{lang_iso} in #{key_name}"
        else
          translation = "<message>#{k["translation"]}</message>"
          validation_result = schema.validate(Nokogiri::XML(translation))
          if validation_result.empty?
            valid_translations[lang_iso] = translation
          else
            error_messages += validation_result.map { |e| "#{lang_iso} in #{key_name} failed schema validation: #{e.message}" }
          end
        end
      end
      return valid_translations, error_messages
    end

    # Parses the XML format contained in the Localise keys and returns a Hash with the message fields.
    def parse_local_message(message_xml)
      document = Nokogiri::XML(message_xml)
      local_message = Hash.new
      local_message["head"] = document.xpath("//message/head").text
      local_message["body"] = document.xpath("//message/body").text
      local_message["content"] = document.xpath("//message/content/para").map do |para|
        parse_local_message_content(para)
      end
      return local_message
    end

    def parse_local_message_content(para)
      text_content = para.children.select { |elem| elem.is_a?(Nokogiri::XML::Element) && elem.name == "text" }.map do |el|
        {
          "type" => "para",
          "text" => el.text,
        }
      end.first
      link_content = para.children.select { |elem| elem.is_a?(Nokogiri::XML::Element) && elem.name == "link" }.map do |el|
        {
          "type" => "para",
          "link" => el["url"],
          "linkText" => el.text,
        }
      end.first
      if text_content
        text_content.merge!(link_content) if link_content
        return text_content
      else
        return link_content
      end
    end

    def construct_local_messages_translations(valid_messages)
      translations = Hash.new
      valid_messages.each { |lang, message| translations[lang] = parse_local_message(message) }
      return translations
    end

    def construct_local_messages_metadata(voc_messages)
      local_messages_payload = {}
      local_messages_payload["messages"] = voc_messages.each_with_object({}) do |message, messages|
        messages[message.first] = { "type" => "notification", "contentVersion" => 1 }
        messages[message.first]["translations"] = construct_local_messages_translations(message.last)
      end
      return local_messages_payload
    end
  end
end
