namespace :export do
  desc "Creates a test key/example bundle to send to Apple & Google"
  task :"key:dev" => [:"clean:wipe"] do
    include NHSx::Export
    include NHSx::TargetEnvironment

    export_location = File.join($configuration.out, "export/keys/dev")
    mkdir_p(export_location, :verbose => false)

    server_key_arn = signing_key_id($configuration)
    bundle_id = "uk.nhs.covid19.internal"
    bundle_name = "#{Time.now.strftime("%Y%m%d_%H%M%S")}-scenario_bundle"

    bundle_location = generate_bundle(
      export_location,
      server_key_arn,
      bundle_id,
      bundle_name
    )
    puts "*" * 74
    puts "* Key bundle created in #{bundle_location}"
  end
  desc "Extracts the public key for the static content signing server key for the dev account as .pem"
  task :"signing:dev" => [:"clean:wipe"] do
    include NHSx::Export
    include NHSx::TargetEnvironment
    export_location = File.join($configuration.out, "export/keys/dev")
    export_file = File.join(export_location, "ContentSigningKey_dev.pem")
    mkdir_p(export_location, :verbose => false)

    server_key_arn = content_signing_key_id($configuration)
    public_key_pem_path = download_public_key_as_pem(export_location, server_key_arn)
    mv(public_key_pem_path, export_file, :verbose => false)
    puts "*" * 74
    puts "Content signing key exported in #{export_file}"
  end

  desc "Creates a prod key/example bundle to send to Apple & Google"
  task :"key:prod" => [:"clean:wipe", :"login:prod"] do
    include NHSx::Export
    include NHSx::TargetEnvironment

    export_location = File.join($configuration.out, "export/keys/prod")
    mkdir_p(export_location, :verbose => false)

    server_key_arn = signing_key_id($configuration)
    bundle_id = "uk.nhs.covid19.production"

    bundle_name = "#{Time.now.strftime("%Y%m%d_%H%M%S")}-prod_bundle"

    bundle_location = generate_bundle(
      export_location,
      server_key_arn,
      bundle_id,
      bundle_name
    )
    puts "*" * 74
    puts "* Key bundle created in #{bundle_location}"
  end
  desc "Extracts the public key for the static content signing server key for the prod account as .pem"
  task :"signing:prod" => [:"clean:wipe", :"login:prod"] do
    include NHSx::Export
    include NHSx::TargetEnvironment
    export_location = File.join($configuration.out, "export/keys/prod")
    export_file = File.join(export_location, "ContentSigningKey_prod.pem")

    mkdir_p(export_location, :verbose => false)
    server_key_arn = content_signing_key_id($configuration)
    public_key_pem_path = download_public_key_as_pem(export_location, server_key_arn)
    mv(public_key_pem_path, export_file, :verbose => false)
    puts "*" * 74
    puts "Content signing key exported in #{export_file}"
  end

  desc "Given a KEY_ARCHIVE it will extract it, verify the signature and print out the key details"
  task :verify do
    include Zuehlke::Package
    include NHSx::Export
    include NHSx::TargetEnvironment

    key_archive_file = ENV["KEY_ARCHIVE"]
    extract_to = File.join($configuration.out, "sandbox", File.basename(key_archive_file))
    rm_rf(extract_to, :verbose => false)
    unpack(key_archive_file, extract_to)

    bin_content = File.read(File.join(extract_to, "export.bin"))

    key_archive = decode_export(bin_content)

    verification_succeeded = true
    begin
      verify_key_export(signing_key_id($configuration), File.join(extract_to, "export.bin"), File.join(extract_to, "export.sig"), $configuration)
    rescue GaudiError
      verification_succeeded = false
    end

    puts "Found #{key_archive.keys.size} keys in the archive:"
    puts "-" * 50
    puts "    Base64 encoding       | Interval number"
    puts "-" * 50
    key_archive.keys.each do |k|
      puts " #{::Base64.encode64(k.key_data).chomp} | #{k.rolling_start_interval_number}"
      puts "-" * 50
    end
    puts "Bundle signature verification FAILED" unless verification_succeeded
  end

  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      desc "Export exposure notification counts to S3 for #{tgt_env}"
      task :"en_count:#{tgt_env}" => [:"login:#{account}"] do
        include NHSx::AWS

        objects = list_objects("te-#{tgt_env}-analytics-en-circuit-breaker", "", $configuration)
        dates = objects.map { |obj| Date.parse(File.dirname(obj["Key"])) }
        # This is the first date for which the logs are structured - query will fail on previous dates
        start_date = Date.parse("2021/03/09")
        end_date = Date.today - 1
        valid_range = (start_date..end_date).to_a

        missing_dates = (valid_range - dates)

        target_config = target_environment_configuration(tgt_env, account, $configuration)
        export_lambda = target_config["exposure_notification_circuit_breaker_analytics_lambda_function_name"]
        puts "Missing #{missing_dates.size} entries in the export logs"

        cli = HighLine.new
        answer = cli.ask "This task will trigger the export for the following dates:\n#{missing_dates.map(&:iso8601).join("\n")}\nType 'continue' to confirm"
        raise GaudiError, "Aborted export operation" unless ["continue"].include?(answer.downcase)

        begin
          update_lambda_env_vars(export_lambda, { "ABORT_OUTSIDE_TIME_WINDOW" => "false" }, $configuration)
          missing_dates.each do |missing_date|
            puts "Exporting data for #{missing_date.iso8601}"
            # The lambda triggers the day after the one we want the data for
            run_for_date = missing_date + 1
            payload = { "time" => run_for_date.iso8601 }
            invoke_lambda(export_lambda, JSON.dump(payload), $configuration)
          end
        ensure
          update_lambda_env_vars(export_lambda, { "ABORT_OUTSIDE_TIME_WINDOW" => "true" }, $configuration)
        end
      end
    end
  end
end
