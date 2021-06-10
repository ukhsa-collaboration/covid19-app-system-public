require "test-unit"
require "mocha/test_unit"
require_relative "../lib/nhsx/helpers/aws"

class AwsTests < Test::Unit::TestCase
  include NHSx::AWS

  def setup_run_command
    run_command_args = []
    stubs(:run_command).with { |*args| run_command_args.push(*args) }
    run_command_args
  end

  def test_uploads_with_expected_command
    run_command_args = setup_run_command

    upload_single_file_to_s3("some-local-file.csv", "some-s3-object-path", { })
    assert_includes(run_command_args, "aws s3 cp some-local-file.csv s3://some-s3-object-path --content-type text/csv")
  end

  def test_uploads_with_expected_command_without_content_type
    run_command_args = setup_run_command

    upload_single_file_to_s3("file", "some-s3-object-path", { })
    assert_includes(run_command_args, "aws s3 cp file s3://some-s3-object-path")
  end

  def test_uploads_recursively
    run_command_args = setup_run_command

    upload_recursively_to_s3("local_dir", "s3-location", { })
    assert_includes(run_command_args, "aws s3 cp local_dir s3://s3-location --recursive")
  end

  def test_download_recursively
    run_command_args = setup_run_command

    download_recursively_from_s3("local_dir", "s3-location", { })
    assert_includes(run_command_args, "aws s3 cp s3://local_dir s3-location --recursive")
  end

  def test_invoke_lambda
    run_command_args = setup_run_command
    Commandlines.stubs(:new_lambda_output_file)

    invoke_lambda("abc-function", "{'a': 'b'}", { })
    assert_includes(run_command_args, "aws --cli-read-timeout 0 --cli-connect-timeout 0 lambda invoke --region eu-west-2 --function-name abc-function --cli-binary-format raw-in-base64-out --payload \\{\\'a\\':\\ \\'b\\'\\} ")
  end

  def test_invoke_lambda_no_payload
    run_command_args = setup_run_command
    Commandlines.stubs(:new_lambda_output_file)

    invoke_lambda("abc-function", "", { })
    assert_includes(run_command_args, "aws --cli-read-timeout 0 --cli-connect-timeout 0 lambda invoke --region eu-west-2 --function-name abc-function  ")
  end

  def test_finds_content_type
    assert_equal("text/csv", content_type_of("file1.csv"))
    assert_equal("application/json", content_type_of("file2.json"))
  end

  def test_raises_when_unable_to_find_content_type
    assert_nil(content_type_of("file.123"))
    assert_nil(content_type_of("file.abc"))
  end

  def test_raises_when_empty_extension
    assert_nil(content_type_of("file"))
    assert_nil(content_type_of("file."))
  end
end
