require "test-unit"
require "mocha/test_unit"
require_relative "../lib/nhsx/helpers/validate"
require "json"

# Tests the validation rules for the local messages format
class ValidateLocalMessagesTest < Test::Unit::TestCase
  include NHSx::Validate

  def test_existence_validation
    local_messages = { "Notification.Voc.Generic" => {} }
    la_mapping = { "ABCD1234" => ["Notification.Voc.Generic"] }
    assert_nothing_raised {
      validate_local_messages(la_mapping, local_messages)
    }
    la_mapping = { "ABCD1234" => ["Notification.Voc.Specific"] }
    assert_raise(GaudiError) { validate_local_messages(la_mapping, local_messages) }
  end

  def test_placeholders_validation
    voc_message_metadata = {
      "messages" => {
        "Notification.Some.Stuff" => {
          "type" => "notification",
          "contentVersion" => 1,
          "translations" => {
            "en" => {
              "head" => "[postcode] some (COVID-19) head text (postcode)",
              "body" => "some body (PCR) text /postcode/\\postcode\\.",
              "content" => [
                {
                  "type" => "para",
                  "text" => "{postcode} some content text 1 -postcode-."
                },
                {
                  "type" => "para",
                  "text" => "some content text *postcode*.",
                  "link" => "https://www.gov.uk/find-local-council",
                  "linkText" => "some content text 3 0postcode0."
                }
              ]
            },
            "cy" => {
              "head" => "some head text",
              "body" => "some body text.",
              "content" => [
                {
                  "type" => "para",
                  "text" => "some content text 1."
                },
                {
                  "type" => "para",
                  "text" => "some content text.",
                  "link" => "https://www.gov.uk/find-local-council",
                  "linkText" => "some content &POSTCODE* text 3."
                }
              ]
            }
          }
        }
      }
    }
    warnings = validate_local_messages_placeholders(voc_message_metadata)
    assert_equal(
      [
        { "Notification.Some.Stuff.translations.en.head" => ["(postcode)"] },
        { "Notification.Some.Stuff.translations.en.body" => %W[/postcode/ \\postcode\\] },
        { "Notification.Some.Stuff.translations.en.content.0.text" => %w[{postcode} -postcode-] },
        { "Notification.Some.Stuff.translations.en.content.1.text" => ["*postcode*"] },
        { "Notification.Some.Stuff.translations.en.content.1.linkText" => ["0postcode0"] },
        { "Notification.Some.Stuff.translations.cy.content.1.linkText" => ["&POSTCODE*"] }
      ], warnings
    )
  end
end
