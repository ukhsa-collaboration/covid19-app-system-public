module Gaudi
  module Configuration
    # Encapsulates the environment variables used to adjust the builder's configuration
    #
    # Is mixed in with SystemConfiguration
    module EnvironmentOptions
      include EnvironmentHelpers
      # Specify the target environment to use when deploying or running tests
      def target_environment
        mandatory("TARGET_ENVIRONMENT")
      end

      # Specify the name for the new api key
      def api_key_name
        mandatory("API_KEY_NAME")
      end

      # Path to a file containing test data. This is a parameter for most publish tasks and the format of the TEST_DATA file depends on the corresponding task.
      def test_data
        test_data_file = File.expand_path(mandatory("TEST_DATA"))
        raise GaudiError, "Test data #{test_data_file} not found" unless File.exist?(test_data_file)

        return test_data_file
      end

      # Path to a file containing static content. This is a parameter for most publish tasks and the format of the STATIC_CONTENT file depends on the corresponding task.
      def static_content
        static_content_file = File.expand_path(mandatory("STATIC_CONTENT"))
        raise GaudiError, "Static content file #{static_content_file} not found" unless File.exist?(static_content_file)

        return static_content_file
      end

      # Path to a file to be uploaded. This is a parameter for most upload tasks and the format of the UPLOAD_DATA file depends on the corresponding upload API.
      def upload_data
        upload_data_file = File.expand_path(mandatory("UPLOAD_DATA"))
        raise GaudiError, "Upload data #{upload_data_file} not found" unless File.exist?(upload_data_file)

        return upload_data_file
      end

      def pubdash_analytics
        "out/pubdash/analytics"
      end

      def pubdash_app_store_dir
        "#{pubdash_analytics}/app_store"
      end

      def pubdash_qr_posters_dir
        "#{pubdash_analytics}/qr_posters"
      end

      def pubdash_postcode_lookup_dir
        "#{pubdash_analytics}/postcode_lookup"
      end

      def pubdash_analytics_parquet_dir
        "#{pubdash_analytics}/analytics_parquet"
      end

      # Pass the value for a TOKEN
      def token
        mandatory("TOKEN")
      end

      # Pass a point in time value for restoring backups
      #
      # Returns epoch time for given value
      #
      # Eg: RESTORE_AT = "September 9, 2020 at 8:32:06 PM UTC+8"
      def restore_at
        restore_time = mandatory("RESTORE_AT")
        parsed_time = DateTime.parse(restore_time)
        return parsed_time.strftime("%s")
      end

      def localise_input
        input_dir = ENV["LOCALISE_INPUT"]
        input_dir ||= File.expand_path("locale")
        input_dir = File.expand_path(input_dir)

        raise GaudiError, "Cannot find localisation input. '#{input_dir}' does not exist" unless File.exist?(input_dir)

        return input_dir
      end

      # Pass test/virology result when invoking the virology token generation lambda (default POSITIVE)
      def test_result
        test_result = ENV.fetch("TEST_RESULT", "POSITIVE")

        raise GaudiError, "Invalid TEST_RESULT" unless ["POSITIVE", "NEGATIVE", "VOID", "PLOD"].include?(test_result)

        return test_result
      end

      # Pass test/virology end date when invoking the virology token generation lambda
      #
      # Eg: TEST_END_DATE=2020-10-10T00:00:00Z
      def test_end_date
        require "time"
        test_end_date = mandatory("TEST_END_DATE")
        raise GaudiError, "Invalid TEST_END_DATE" unless Time.iso8601(test_end_date)

        return test_end_date
      end

      # Pass number of tokens to generate when invoking the virology token generation lambda (default 35000)
      def number_of_tokens
        number_of_tokens = mandatory("NUMBER_OF_TOKENS")

        raise GaudiError, "Invalid NUMBER_OF_TOKENS" unless number_of_tokens

        return number_of_tokens
      end

      # Pass test/virology start date when invoking the virology token generation lambda
      #
      # Eg: START_DATE=2020-10-10T00:00:00Z
      def start_date
        require "time"
        start_date = mandatory("START_DATE")
        raise GaudiError, "Invalid START_DATE" unless Time.iso8601(start_date)

        return start_date
      end

      # Pass test/virology number of days when invoking the virology token generation lambda
      #
      # Eg: NUMBER_OF_DAYS=7
      def number_of_days
        number_of_days = mandatory("NUMBER_OF_DAYS")
        number_of_days = number_of_days.to_i
        raise GaudiError, "Invalid NUMBER_OF_DAYS" unless number_of_days

        return number_of_days
      end

      # Pass input file to a task
      #
      # Eg: INPUT_FILE=path/to/file
      def input_file
        input_file = mandatory("INPUT_FILE")
        raise GaudiError, "Missing INPUT_FILE" unless input_file
        return input_file
      end

      # Pass the batch number for the subscription (it can be 1, 2 or 3)
      def batch_number
        batch_number = mandatory("BATCH_NUMBER")
        batch_number = batch_number.to_i

        raise GaudiError, "Invalid BATCH_NUMBER" unless [1, 2, 3].include?(batch_number)

        return batch_number
      end

      # Pass the email id for the subscription
      def email
        email = mandatory("EMAIL")

        raise GaudiError, "Invalid EMAIL" unless email

        return email
      end

      # Pass the mobile number for the subscription (it should include the country code)
      def mobile_number
        mobile_number = mandatory("MOBILE_NUMBER")

        raise GaudiError, "Invalid MOBILE_NUMBER" unless mobile_number

        return mobile_number
      end

      def cta_token
        token = mandatory("TOKEN")
        raise GaudiError, "Invalid TOKEN" unless token

        return token
      end

      def polling_token
        token = mandatory("POLLING_TOKEN")
        raise GaudiError, "Invalid POLLING_TOKEN" unless token

        return token
      end

      def test_kit
        test_kit = ENV.fetch("TEST_KIT", "LAB_RESULT")
        valid_values = ["LAB_RESULT", "RAPID_RESULT", "RAPID_SELF_REPORTED"]
        raise GaudiError, "Invalid TEST_KIT, valid input is #{valid_values.join(", ")}" unless valid_values.include?(test_kit)

        return test_kit
      end

      # Pass the value for a ACCOUNT
      def account
        ENV.fetch("ACCOUNT", "dev")
      end

      # Pass the value for a BRANCH
      #
      # This is aware of execution in a CodeBuild environment so it will return CODEBUILD_SOURCE_VERSION instead of BRANCH in CodeBuild
      def branch
        branch_name = ENV["CODEBUILD_SOURCE_VERSION"]
        branch_name ||= ENV.fetch("BRANCH", "master")

        return branch_name
      end

      def print_logs
        return ENV.fetch("PRINT_LOGS", false)
      end

      # Pass the value for a CodeBuild job id
      def job_id
        return mandatory("JOB_ID")
      end

      # Pass a version number to be used for a release.
      def release_version(version_metadata)
        version_number = mandatory("RELEASE_VERSION").gsub(",", ".")

        raise GaudiError, "The release #{version_number} already exists" if tag_exists?("#{version_metadata["SubsystemPrefix"]}#{version_number}", self)
        raise GaudiError, "Only provide the version number, e.g. 2.3" if version_number.gsub(".", "").to_f.zero?

        return version_number
      end

      # Returns the value of FROM_VERSION or the last backend release tag
      def from_version(version_metadata)
        return ENV["FROM_VERSION"] if ENV["FROM_VERSION"]

        source_commit = "Backend-#{version_metadata["Major"]}.#{version_metadata["Minor"]}"
        return source_commit
      end

      # Returns the value of TO_VERSION or HEAD
      def to_version
        ENV.fetch("TO_VERSION", current_sha)
      end

      # Path to a file containing static content. This is a parameter for most publish tasks and the format of the MESSAGE_MAPPING file depends on the corresponding task.
      def message_mapping(system_config)
        static_content_file = File.expand_path(ENV.fetch("MESSAGE_MAPPING", "#{system_config.base}/src/static/local-authority-message-mapping.json"))
        raise GaudiError, "Static content file #{static_content_file} not found" unless File.exist?(static_content_file)

        return static_content_file
      end

      # Path to a file containing static content. This is a parameter for most publish tasks and the format of the MESSAGES_METADATA file depends on the corresponding task.
      def messages_metadata(system_config)
        static_content_file = File.expand_path(ENV.fetch("MESSAGES_METADATA", "#{system_config.base}/src/static/local-messages-metadata.json"))
        raise GaudiError, "Static content file #{static_content_file} not found" unless File.exist?(static_content_file)

        return static_content_file
      end

      # Pass export start date when invoking the analytics logs export lambda
      #
      # Eg: EXPORT_START_DATE=2020/10/10
      def export_start_date
        export_start_date = mandatory("EXPORT_START_DATE")
        raise GaudiError, "Invalid EXPORT_START_DATE" unless export_start_date

        return export_start_date
      end

      # Pass export end date when invoking the analytics logs export lambda
      #
      # Eg: EXPORT_END_DATE=2020/10/10
      def export_end_date
        export_end_date = mandatory("EXPORT_END_DATE")
        raise GaudiError, "Invalid EXPORT_END_DATE" unless export_end_date

        return export_end_date
      end
    end
  end
end
