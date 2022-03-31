require_relative "gen"

module NHSx
  # Helpers that codify integration and system test execution against target environments
  module Test
    include Zuehlke::Execution
    include NHSx::Generate

    def run_target_environment_smoke_tests(target_environment, account_name, system_config)
      test_config = generate_test_config(target_environment, account_name, system_config)
      java_project_path = File.join(system_config.base, "src/aws/lambdas/incremental_distribution")
      gradlew = File.join(java_project_path, "gradlew")
      cmdline = "SMOKE_TEST_CONFIG=#{test_config} #{gradlew} --rerun-tasks --console plain -p #{java_project_path} testSmoke"
      run_tee("Runs JVM smoke tests", cmdline, system_config)
    end

    def run_target_environment_integration_tests(target_environment)
      java_project_path = File.join($configuration.base, "src/aws/lambdas/incremental_distribution")
      gradlew = File.join(java_project_path, "gradlew")
      cmdline = "INTEGRATION_TEST_ENV=te-#{target_environment} #{gradlew} --rerun-tasks --console plain -p #{java_project_path} cta:testIntegration"
      run_tee("Runs JVM integration tests", cmdline, $configuration)
    end

    def run_target_environment_sanity_tests(test_task, target_environment, account_name, system_config)
      test_config = generate_test_config(target_environment, account_name, system_config)
      java_project_path = File.join(system_config.base, "test/sanitybot")
      gradlew = File.join(java_project_path, "gradlew")
      cmdline = "SANITY_TEST_CONFIG=#{test_config} TARGET_ENVIRONMENT=#{account_name} #{gradlew} --rerun-tasks --console plain -p #{java_project_path} #{test_task}"
      run_tee("Runs JVM #{test_task} tests", cmdline, system_config)
    end

    def run_target_environment_analytics_sanity_tests(test_task, target_environment, account_name, system_config)
      test_config = generate_analytics_test_config(target_environment, account_name, system_config)
      java_project_path = File.join(system_config.base, "test/analytics-sanitybot")
      gradlew = File.join(java_project_path, "gradlew")
      cmdline = "ACCOUNT=#{account_name} TARGET_WORKSPACE=#{target_environment} SANITY_TEST_CONFIG=#{test_config} #{gradlew} --rerun-tasks --console plain -p #{java_project_path} #{test_task}"
      run_tee("Runs JVM #{test_task} tests", cmdline, system_config)
    end

    def run_target_unit_tests
      java_project_path = File.join($configuration.base, "src/aws/lambdas/incremental_distribution")
      gradlew = File.join(java_project_path, "gradlew")
      cmdline = "#{gradlew} --rerun-tasks --console plain -p #{java_project_path} test"
      run_tee("Runs java unit tests", cmdline, $configuration)
    end
  end
end
