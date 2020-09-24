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
end
