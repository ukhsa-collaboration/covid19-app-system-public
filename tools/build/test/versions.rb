require "test-unit"
require "mocha/test_unit"
require_relative "../lib/nhsx/helpers/versions"

class VersionsTests < Test::Unit::TestCase
  include NHSx::Versions

  def test_pointer_tags
    assert_equal("te-ci", pointer_tag_name("backend", "ci"))
    assert_equal("te-ci", pointer_tag_name("cta", "ci"))
    (NHSx::Versions::SUBSYSTEMS.keys - ["backend", "cta"]).each do |sb|
      assert_equal("te-ci-#{sb}", pointer_tag_name(sb, "ci"))
    end
    assert_raise(GaudiError) {
      pointer_tag_name("foo", "ci")
    }
  end

  def test_label_tags
    assert_equal("Backend-2.9", label_tag_name("backend", "2.9"))
    assert_equal("Backend-2.9", label_tag_name("cta", "2.9"))
    assert_equal("Analytics-2.9", label_tag_name("analytics", "2.9"))
    assert_equal("Tiers-2.9", label_tag_name("tiers", "2.9"))
    assert_equal("Doreto-2.9", label_tag_name("doreto", "2.9"))
    assert_equal("Availability-2.9", label_tag_name("availability", "2.9"))
    assert_equal("PublicDashboard-2.9", label_tag_name("pubdash", "2.9"))
    assert_raise(GaudiError) {
      label_tag_name("foo", "2.9")
    }
  end
end
