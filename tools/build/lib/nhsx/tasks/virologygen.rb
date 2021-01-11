namespace :virology do
  namespace :gen do
    NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        desc "Generate virology tokens for a single date with TEST_END_DATE, TEST_RESULT and NUMBER_OF_TOKENS"
        task :"single:#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::Virology
          include NHSx::Generate
          config_file = JSON.parse(File.read(generate_test_config(tgt_env, account, $configuration)))
          generate_single(config_file, $configuration)
        end
        desc "Generate virology tokens for an interval of dates with START_DATE, NUMBER_OF_DAYS, TEST_RESULT and NUMBER_OF_TOKENS"
        task :"interval:#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::Virology
          include NHSx::Generate
          config_file = JSON.parse(File.read(generate_test_config(tgt_env, account, $configuration)))
          generate_interval(config_file, $configuration)
        end
      end
    end
  end
end
