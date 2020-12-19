namespace :test do
  NHSx::TargetEnvironment::TARGET_ENVIRONMENTS["dev"].each do |tgt_env|
    desc "Runs all maven smoke tests against the #{tgt_env} target environment"
    task :"maven:smoke:#{tgt_env}" do
      include NHSx::Test
      run_target_environment_smoke_tests(tgt_env, "dev", $configuration)
    end
  end

  desc "Runs all java unit tests"
  task :"java:unit" do
    include NHSx::Test
    run_target_unit_tests
  end

  desc "Runs the sanity_check tests against prod"
  task :"sanity_check:prod" => [:"login:prod", :"clean:config"] do
    include NHSx::Test
    run_target_environment_sanity_tests("testSanity", "prod", "prod", $configuration)
  end

  desc "Runs the sanity_check tests against staging"
  task :"sanity_check:staging" => [:"login:staging", :"clean:config"] do
    include NHSx::Test
    run_target_environment_sanity_tests("testSanity", "staging", "staging", $configuration)
  end

  # Generate separate tasks per named environment
  NHSx::TargetEnvironment::TARGET_ENVIRONMENTS["dev"].each do |tgt_env|
    desc "Runs the sanity_check tests against #{tgt_env} "
    task :"sanity_check:#{tgt_env}" => [:"login:dev", :"clean:config"] do
      include NHSx::Test
      run_target_environment_sanity_tests("testSanity", tgt_env, "dev", $configuration)
    end
  end

end
