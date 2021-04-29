require "test-unit"
require "mocha/test_unit"
require_relative "../lib/nhsx/helpers/login"

# Tests the login process to the various AWS accounts used
class LoginTests < Test::Unit::TestCase
  include NHSx::Login

  # Login to an account with SSO from a developer workstation
  def test_login_to_sso_account_as_dev
    domain = "Whatever"
    account = "prod"

    ENV.delete("CODEBUILD_BUILD_ID")
    expects(:sso_login).with(account, domain).returns("profile")
    login_to_sso_account(account, domain, false)
    assert_equal(account, ENV["ACCOUNT"], "Account environment variable not correctly set")
    assert_equal("profile", ENV["AWS_PROFILE"])
  end

  # Login to an account with SSO from within AWS CodeBuild
  def test_login_to_sso_account_as_aws
    domain = "whatever"
    account = "prod"
    ENV["CODEBUILD_BUILD_ID"] = "codebuild:foo"
    expects(:sso_login).with(account, domain).returns("profile")
    login_to_sso_account(account, domain, false)
    assert_equal(account, ENV["ACCOUNT"], "Account environment variable not correctly set")
    assert_equal("profile", ENV["AWS_PROFILE"])
  end

  # Login to an account with SSO protected with a confirmation prompt (i.e. prod)
  def test_login_to_sso_account_with_prompt
    domain = "whatever"
    account = "prod"
    expects(:double_check_prompt).with(account).returns(true)
    expects(:sso_login).with(account, domain).returns("profile")
    login_to_sso_account(account, domain, true)
    assert_equal(account, ENV["ACCOUNT"], "Account environment variable not correctly set")
    assert_equal("profile", ENV["AWS_PROFILE"])
  end

  def test_container_guard
    system_config = mock
    system_config.expects(:base).returns("/workspace")

    assert_nothing_raised { container_guard(system_config) }

    system_config.expects(:base).returns("/foo")
    ENV["CODEBUILD_BUILD_ID"] = "codebuild:foo"
    assert_nothing_raised { container_guard(system_config) }

    system_config.expects(:base).returns("/foo")
    ENV.delete("CODEBUILD_BUILD_ID")
    assert_raises(GaudiError) { container_guard(system_config) }
  end

  def test_login_to_aws_account_sso
    domain = "test"
    account = "aa-prod"
    expects(:double_check_prompt).with(account).returns(true)
    expects(:sso_login).with(account, domain).returns("profile")
    login_to_aws_account(account, domain, true)

    assert_equal(account, ENV["ACCOUNT"], "Account environment variable not correctly set")
    assert_equal("profile", ENV["AWS_PROFILE"])
  end

  def test_login_to_aws_account_mfa
    domain = "test"
    account = "prod"
    expects(:double_check_prompt).with(account).returns(true)
    expects(:mfa_login).with(account).returns("profile")
    login_to_aws_account(account, domain, true)

    assert_equal(account, ENV["ACCOUNT"], "Account environment variable not correctly set")
    assert_equal("profile", ENV["AWS_PROFILE"])
  end

  def test_with_account
    login_to_aws_account("dev", "test", false)
    assert_equal("dev", ENV["ACCOUNT"], "AWS settings not correctly setup")
    expects(:mfa_login).with("prod").returns("profile")
    with_account("prod", "test") do
      assert_equal("prod", ENV["ACCOUNT"], "AWS settings not correctly set within the block")
    end
    assert_equal("dev", ENV["ACCOUNT"], "AWS settings not correctly restored")
  end
end
