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

  desc "Download the code build artifacts"
  task :"codebuild:dev" => [:"login:dev", :"clean:wipe"] do
    include NHSx::Report

    # get build info
    job_id = $configuration.job_id
    build_info = build_info([job_id])

    # download zip from s3 bucket
    downloads_out_dir = File.join($configuration.out, "downloads/")
    object_name = build_info.first["artifacts"]["location"]
    object_name = object_name.sub("arn:aws:s3:::", "")
    zip_file_path = File.join(downloads_out_dir, "#{object_name}.zip")
    if object_name == "dev-build-artifacts-archive/ci-app-system"
      puts "No upload artifacts present for build #{job_id}"
    else
      run_command("Download the build artifacts of #{job_id}", NHSx::AWS::Commandlines.download_from_s3(object_name, zip_file_path), $configuration)
      # unzip to base dir
      run_command("Unzip archive", "unzip #{zip_file_path} -d #{$configuration.base}", $configuration)
      # remove downloaded s3 zip file
      File.delete(zip_file_path)
    end
    include NHSx::Queue #redefines build_info - needs to be cleaned up
    bi = NHSx::Queue::CodeBuildInfo.new(build_info.first)
    pipe_logs(bi)
  end
end
