require_relative "gen"

module NHSx
  # Helpers that codify integration and system test execution against target environments
  module Test
    include Zuehlke::Execution
    include NHSx::Generate
    # Location of the .robot specs relative to the repository workspace root
    ROBOT_TEST_LOCATION = "test/robot/suites".freeze
    # Return the names of all target environment test suites found in the repository
    def self.robot_target_environment_test_suites(system_config)
      Rake::FileList["#{system_config.base}/#{ROBOT_TEST_LOCATION}/target_environment/*"].pathmap("%n")
    end

    # Runs one or more robot test suites with the given test configuration
    #
    # The robot test suites are organised in directories following the
    #
    #   test_category/test_suite_name
    #
    # scheme under test/robot/suites
    def run_robot_tests(robot_test_suites, test_config, test_category, system_config)
      report_name = "all"
      report_name = robot_test_suites.first if robot_test_suites.size == 1

      test_output = File.join(system_config.out, "reports", report_name)
      test_location = File.join(system_config.base, ROBOT_TEST_LOCATION, test_category)
      to_include = robot_test_suites.join(" --suite ") # --suite can be repeated any number of times
      cmdline = "robot --console dotted --outputdir #{test_output} --variable TEST_CONFIGURATION_FILE:\"#{test_config}\" --suite #{to_include} #{test_location}"

      run_command("Run #{report_name} tests", cmdline, system_config)
    end

    # Runs the named test suite against the target environment
    def run_target_environment_test_suites(test_suites, target_environment, account_name, system_config)
      test_config = generate_test_config(target_environment, account_name, system_config)
      run_robot_tests(test_suites, test_config, "target_environment", system_config)
    end

    def run_target_environment_smoke_tests(target_environment, account_name, system_config)
      test_config = generate_test_config(target_environment, account_name, system_config)
      java_project_path = File.join($configuration.base, "src/aws/lambdas/incremental_distribution")
      pom_xml_path = File.join(java_project_path, "pom.xml")
      cmdline = "SMOKE_TEST_CONFIG=#{test_config} mvn -P smokeProfile -f=#{pom_xml_path} test"
      run_command("Runs maven smoke tests", cmdline, $configuration)
    end

    def run_target_unit_tests
      java_project_path = File.join($configuration.base, "src/aws/lambdas/incremental_distribution")
      pom_xml_path = File.join(java_project_path, "pom.xml")
      java_output_path = File.join($configuration.out, "java/batch_creation")
      cmdline = "mvn -P buildProfile -f=#{pom_xml_path} -DbuildOutput=#{java_output_path} test"
      run_command("Runs java smoke tests", cmdline, $configuration)
    end
  end
end
