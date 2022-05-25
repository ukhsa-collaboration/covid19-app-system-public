require "test-unit"
require "mocha/test_unit"
require "json"
require_relative "../lib/nhsx/helpers/import"

class ImportTests < Test::Unit::TestCase
  include NHS::Import

  def test_successful_conversion_of_xml_to_json
    xml = <<-EOT
      <message>
        <head>A new variant of concern is in your area</head>
        <body>Here is the body of the notification</body>
        <content>
          <para>
            <text>There have been reported cases of a new variant in {postcode}.</text>
          </para>
          <para>
            <text>Here are some key pieces of information to help you stay safe.</text>
            <link url="https://example.com">Find out about testing</link>
          </para>
        </content>
      </message>
    EOT

    json = <<-EOT
      {"head": "A new variant of concern is in your area",
      "body": "Here is the body of the notification",
      "content": [
        {
          "type": "para",
          "text": "There have been reported cases of a new variant in {postcode}."
        },
        {
          "type": "para",
          "text": "Here are some key pieces of information to help you stay safe.",
          "link": "https://example.com",
          "linkText": "Find out about testing"
        }]}
    EOT
    assert_equal(parse_local_message(xml), JSON.parse(json))
  end

  def test_sucessful_extraction_lokalise
    translations = <<-EOT
    [{"translation_id": 660851013,
  "key_id": 91693898,
  "language_iso": "zh_CN",
  "translation": "<head></head><body></body><content>   <para>    <text></text>  </para>  <para>    <text></text>    <link url=\\"https://example.com\\"></link>  </para></content>",
  "modified_by": 106775,
  "modified_by_email": "james.richardson28@test-and-trace.nhs.uk",
  "modified_at": "2021-05-20 13:29:33 (Etc/UTC)",
  "modified_at_timestamp": 1621517373,
  "is_reviewed": false,
  "reviewed_by": 0,
  "is_unverified": false,
  "is_fuzzy": false,
  "words": 1,
  "custom_translation_statuses": [],
  "task_id": null
}]
    EOT
    lokalise_client = mock
    Lokalise.expects(:client).returns(lokalise_client)
    api_key = "dummy_api_key"
    lokalise_key = mock
    lokalise_key.expects(:key_id).returns("dummy_key")
    lokalise_key.expects(:key_name).returns("dummy_key")
    translations_mock = mock
    translations_mock.expects(:translations).returns(JSON.load(translations))
    lokalise_client.expects(:key).returns(translations_mock)
    system_config = mock
    system_config.expects(:base).returns(File.join(File.dirname(__FILE__), "../../.."))
    translations, errors = extract_local_message_translations(api_key, lokalise_key, "dummy_project", system_config)
    assert_equal(1, translations.size)
    assert(errors.empty?)
  end
end
