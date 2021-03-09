require "test-unit"
require "mocha/test_unit"
require_relative "../lib/nhsx/helpers/login"

# def clean_aws_settings(profile_name, sso_profile_name)
#   config = IniFile.load(NHSx::AWS::AWS_CONFIG_PATHS["config"])
#   config.delete_section("profile #{sso_profile_name}")
#   config.save

#   credentials = IniFile.load(NHSx::AWS::AWS_CONFIG_PATHS["credentials"])
#   credentials.delete_section(profile_name)
#   credentials.save
# end

# def do_test_sso_profile(role_name, account, region)
#   profile_info = sso_profile_info(role_name, "analytics", account)
#   sso_profile(profile_info, region)
# end

# Tests the login process to the various AWS accounts used
class LoginTests < Test::Unit::TestCase
  include NHSx::Login

  # Login to an account with SSO from a developer workstation
  def test_login_to_sso_account_as_dev
    role = "ValidRole"
    domain = "Whatever"
    account = "prod"

    ENV.delete("CODEBUILD_BUILD_ID")
    expects(:sso_login).with(role, account, domain).returns("profile")
    login_to_sso_account(role, account, domain, false)
    assert_equal(account, ENV["ACCOUNT"], "Account environment variable not correctly set")
    assert_not_nil(ENV["AWS_PROFILE"])
  end

  # Login to an account with SSO from within AWS CodeBuild
  def test_login_to_sso_account_as_aws
    role = "ValidRole"
    domain = "whatever"
    account = "prod"
    ENV["CODEBUILD_BUILD_ID"] = "codebuild:foo"
    expects(:sso_login).with(role, account, domain).returns("profile")
    login_to_sso_account(role, account, domain, false)
    assert_equal(account, ENV["ACCOUNT"], "Account environment variable not correctly set")
    assert_not_nil(ENV["AWS_PROFILE"])
  end

  # Login to an account with SSO protected with a confirmation prompt (i.e. prod)
  def test_login_to_sso_account_with_prompt
    role = "ValidRole"
    domain = "whatever"
    account = "prod"
    expects(:double_check_prompt).with(account).returns(true)
    expects(:sso_login).with(role, account, domain).returns("profile")
    login_to_sso_account(role, account, domain, true)
    assert_equal(account, ENV["ACCOUNT"], "Account environment variable not correctly set")
    assert_not_nil(ENV["AWS_PROFILE"])
  end

  def test_container_guard
    system_config = mock()
    system_config.expects(:base).returns("/workspace")

    assert_nothing_raised() { container_guard(system_config) }

    system_config.expects(:base).returns("/foo")
    ENV["CODEBUILD_BUILD_ID"] = "codebuild:foo"
    assert_nothing_raised() { container_guard(system_config) }

    system_config.expects(:base).returns("/foo")
    ENV.delete("CODEBUILD_BUILD_ID")
    assert_raises(GaudiError) { container_guard(system_config) }
  end
end
