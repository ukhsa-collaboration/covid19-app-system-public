namespace :backup do
  NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      desc "Sync the diagnosis keys submission bucket for #{tgt_env} locally"
      task :"s3:#{tgt_env}" => [:"login:#{account}"] do
        include NHSx::Generate

        test_config_file = generate_test_config(tgt_env, account, $configuration)
        sync_base = File.join($configuration.out, "backups/#{tgt_env}")
        test_config = JSON.parse(File.read(test_config_file))
        diagnosis_keys_submission_store = test_config["diagnosis_keys_submission_store"]

        run_command("Sync diagnosis keys submission store from #{tgt_env}", "aws s3 sync s3://#{diagnosis_keys_submission_store} #{sync_base}/#{diagnosis_keys_submission_store}", $configuration)
      end
    end
  end
end
