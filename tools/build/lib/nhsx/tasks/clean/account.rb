namespace :clean do
  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each_key do |account|
    desc "Remove the API keys used for tests from the #{account} account"
    task :"test:secrets:#{account}" => [:"login:#{account}"] do
      include NHSx::TargetEnvironment

      tst_secrets = testing_secrets($configuration)
      tst_secrets.each do |test_secret|
        delete_test_secret(test_secret, $configuration)
      end
      zero_test_authentication_headers($configuration)
    end
  end

  task :config do
    rm_rf(File.join($configuration.out, "gen/config"), :verbose => false)
  end
end
