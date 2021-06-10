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
end
