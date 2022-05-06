namespace :virology do
  namespace :gen do
    NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        desc "Generate virology tokens for a single date with TEST_END_DATE, TEST_RESULT, NUMBER_OF_TOKENS and TEST_KIT"
        task :"single:#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::Virology
          include NHSx::Generate
          config_file = JSON.parse(File.read(generate_test_config(tgt_env, account, $configuration)))
          generate_single(config_file, $configuration)
        end
        desc "Generate virology tokens for an interval of dates with START_DATE, NUMBER_OF_DAYS, TEST_RESULT, NUMBER_OF_TOKENS and TEST_KIT"
        task :"interval:#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::Virology
          include NHSx::Generate
          config_file = JSON.parse(File.read(generate_test_config(tgt_env, account, $configuration)))
          generate_interval(config_file, $configuration)
        end
      end
    end
  end

  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      desc "Subscribe to the sns topics used by CTA token automation with BATCH_NUMBER, MOBILE_NUMBER and EMAIL_ID"
      task :"subscribe:#{tgt_env}" => [:"login:#{account}"] do
        include NHSx::Virology
        include NHSx::Generate
        config_file = JSON.parse(File.read(generate_test_config(tgt_env, account, $configuration)))
        mobile_number = $configuration.mobile_number
        batch_number = $configuration.batch_number
        email = $configuration.email
        subscribe_mobile_number_to_topic(mobile_number,batch_number,config_file,$configuration)     
        subscribe_email_to_topic(email,batch_number,config_file,$configuration) 
      end   
    end
  end    
end
