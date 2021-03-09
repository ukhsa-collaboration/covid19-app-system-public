require "test-unit"
require "mocha/test_unit"
require_relative "../lib/nhsx/helpers/codebuild"

class CodeBuildTests < Test::Unit::TestCase
  include NHSx::CodeBuild

  def setup_mock_build_queue(build_ids, builds_in_queue)
    build_queue = build_ids.map do |build_id|
      m = mock(build_id)
      m.responds_like_instance_of(NHSx::CodeBuild::CodeBuildInfo)

      if ["top", "queued", "running"].include?(build_id)
        m.stubs(:completed?).returns(false)
        m.stubs(:stopped?).returns(false)
      elsif build_id == "stopped"
        m.stubs(:completed?).returns(true)
        m.stubs(:stopped?).returns(true)
      else
        m.stubs(:completed?).returns(true)
        m.stubs(:stopped?).returns(false)
      end
      m.stubs(:build_id).returns(build_id)
      stubs(:build_info).with(build_id).returns(m)
      m
    end

    expects(:stop_build).times(builds_in_queue)
    return build_queue
  end

  def test_trim_long_queue
    build_queue = setup_mock_build_queue(["top", "queued", "queued", "queued", "stopped", "running", "stopped", "stopped", "finished", "finished"], 3)
    last_build, running_build = trim_build_queue(build_queue)
    assert_not_nil(running_build)
    assert_equal("running", running_build.build_id)
    assert_equal("top", last_build.build_id)
  end

  def test_trim_no_queue
    build_queue = setup_mock_build_queue(["top", "finished"], 0)
    last_build, running_build = trim_build_queue(build_queue)
    assert_not_nil(last_build)
    assert_not_nil(running_build)
    assert_equal("top", running_build.build_id)
    assert_equal("top", last_build.build_id)
  end

  def test_trim_first_in_the_queue
    build_queue = setup_mock_build_queue(["top", "running", "finished"], 0)
    last_build, running_build = trim_build_queue(build_queue)
    assert_not_nil(last_build)
    assert_not_nil(running_build)
    assert_equal("running", running_build.build_id)
    assert_equal("top", last_build.build_id)
  end
end
