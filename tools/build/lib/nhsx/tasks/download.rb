namespace :download do
  NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      desc "Sync the diagnosis keys two-hourly zips for #{tgt_env} locally"
      task :"dg-2h:#{tgt_env}" => [:"login:#{account}"] do
        include NHSx::Generate

        test_config_file = generate_test_config(tgt_env, account, $configuration)
        sync_base = File.join($configuration.out, "downloads/distribution/#{tgt_env}")
        test_config = JSON.parse(File.read(test_config_file))
        diagnosis_keys_distribution_store = test_config["diagnosis_keys_distribution_store"]
        cmdline = "aws s3 sync s3://#{diagnosis_keys_distribution_store}/distribution/two-hourly/ #{sync_base}/#{diagnosis_keys_distribution_store}/2h"
        run_command("Sync 2h diagnosis keys distribution store from #{tgt_env}", cmdline, $configuration)
        puts "Batches synced in #{sync_base}/#{diagnosis_keys_distribution_store}/2h"
      end

      desc "Sync the diagnosis keys daily zips for #{tgt_env} locally"
      task :"dg-daily:#{tgt_env}" => [:"login:#{account}"] do
        include NHSx::Generate

        test_config_file = generate_test_config(tgt_env, account, $configuration)
        sync_base = File.join($configuration.out, "downloads/distribution/#{tgt_env}")
        test_config = JSON.parse(File.read(test_config_file))
        diagnosis_keys_distribution_store = test_config["diagnosis_keys_distribution_store"]
        cmdline = "aws s3 sync s3://#{diagnosis_keys_distribution_store}/distribution/daily/ #{sync_base}/#{diagnosis_keys_distribution_store}/daily"
        run_command("Sync daily diagnosis keys distribution store from #{tgt_env}", cmdline, $configuration)
        puts "Batches synced in #{sync_base}/#{diagnosis_keys_distribution_store}/daily"
      end

      desc "Download tier information metadata locally"
      task :"tier_metadata:#{tgt_env}" => [:"login:#{account}"] do
        include NHSx::TargetEnvironment
        post_districts_out_dir = File.join($configuration.out, "downloads/tiers")

        target_config = target_environment_configuration(tgt_env, account, $configuration)
        object_name = "#{target_config["post_districts_distribution_store"]}/tier-metadata"
        local_target = File.join(post_districts_out_dir, "tier-metadata.json")
        run_command("Download tier meta data of #{tgt_env} deployment", NHSx::AWS::Commandlines.download_from_s3(object_name, local_target), $configuration)
      end

      desc "Download post district v2 data locally"
      task :"post-districts:#{tgt_env}" => [:"login:#{account}"] do
        include NHSx::TargetEnvironment
        post_districts_out_dir = File.join($configuration.out, "downloads/")

        target_config = target_environment_configuration(tgt_env, account, $configuration)
        object_name = "#{target_config["post_districts_distribution_store"]}/distribution/risky-post-districts-v2"
        local_target = File.join(post_districts_out_dir, "#{tgt_env}-arearisk.json")
        run_command("Download area risk data of #{tgt_env} deployment", NHSx::AWS::Commandlines.download_from_s3(object_name, local_target), $configuration)
      end
    end
  end
end
