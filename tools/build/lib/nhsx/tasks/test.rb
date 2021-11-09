namespace :test do
  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS["dev"].each do |tgt_env|
    desc "Runs all smoke tests against the #{tgt_env} target environment"
    task :"smoke:#{tgt_env}" => [:"login:dev"] do
      include NHSx::Test
      run_target_environment_smoke_tests(tgt_env, "dev", $configuration)
    end
  end

  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS["dev"].each do |tgt_env|
    desc "Runs all integration tests against the #{tgt_env} target environment"
    task :"integration:#{tgt_env}" => [:"login:dev"] do
      include NHSx::Test
      run_target_environment_integration_tests(tgt_env)
    end
  end

  desc "Runs all java unit tests"
  task :"java:unit" do
    include NHSx::Test
    run_target_unit_tests
  end

  # Generate separate tasks per named environment
  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      desc "Runs the cta sanity_check tests against #{tgt_env} "
      task :"sanity_check:#{tgt_env}" => [:"login:#{account}", :"clean:config"] do
        include NHSx::Test
        run_target_environment_sanity_tests("testSanity", tgt_env, account, $configuration)
      end
    end
  end

  NHSx::TargetEnvironment::ANALYTICS_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      desc "Runs the analytics sanity_check tests against #{tgt_env} "
      task :"sanity_check:analytics:#{tgt_env}" => [:"login:#{account}", :"clean:config"] do
        include NHSx::Test
        run_target_environment_analytics_sanity_tests("testSanity", tgt_env, account, $configuration)
      end
    end
  end

  desc "Run the build system unit tests"
  task :build_system do
    test_files = Rake::FileList["#{$configuration.base}/tools/build/test/*.rb"]
    test_failed = false
    test_files.each do |tf|
      begin
        sh("ruby #{tf}")
      rescue
        test_failed = true
      end
    end

    raise GaudiError, "Tests failed" if test_failed
  end
end
