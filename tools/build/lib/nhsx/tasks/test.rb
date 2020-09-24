namespace :test do
  desc "Run all security checks for the account"
  task :account_security do
    include NHSx::Test
    run_robot_tests(["account"], "", "account", $configuration)
  end
  # Generate separate tasks per test suite and named environment
  NHSx::Test.robot_target_environment_test_suites($configuration).each do |test_suite|
    NHSx::TargetEnvironment::TARGET_ENVIRONMENTS["dev"].each do |tgt_env|
      desc "Runs the #{test_suite} tests against the #{tgt_env} target environment"
      task :"#{test_suite}:#{tgt_env}" do
        include NHSx::Test
        run_target_environment_test_suites([test_suite], tgt_env, "dev", $configuration)
      end
    end
  end
  # Generates the tasks for running all test suites together against a target environment
  NHSx::TargetEnvironment::TARGET_ENVIRONMENTS["dev"].each do |tgt_env|
    desc "Runs all tests against the #{tgt_env} target environment"
    task :"all:#{tgt_env}" do
      include NHSx::Test
      test_suites = NHSx::Test.robot_target_environment_test_suites($configuration)
      to_exclude = ["sanity_check", "sanity_check_dev", "sanity_check_prod"]
      to_run = test_suites.reject { |w| to_exclude.include? w }
      run_target_environment_test_suites(to_run, tgt_env, "dev", $configuration)
    end
  end

  NHSx::TargetEnvironment::TARGET_ENVIRONMENTS["dev"].each do |tgt_env|
    desc "Runs all maven smoke tests against the #{tgt_env} target environment"
    task :"maven:smoke:#{tgt_env}" do
      include NHSx::Test
      run_target_environment_smoke_tests(tgt_env, "dev", $configuration)
    end
  end

  desc "Runs the sanity_check tests against prod"
  task :"sanity_check:prod" => [:"login:prod", :"clean:config"] do
    include NHSx::Test
    run_target_environment_test_suites(["sanity_check", "sanity_check_prod"], "prod", "prod", $configuration)
  end
  desc "Runs the sanity_check tests against staging"
  task :"sanity_check:staging" => [:"login:staging", :"clean:config"] do
    include NHSx::Test
    run_target_environment_test_suites(["sanity_check", "sanity_check_prod"], "staging", "staging", $configuration)
  end
end
