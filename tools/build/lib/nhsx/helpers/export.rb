require_relative "../../../../protobuf/ruby/lib/exposure_pb"
require "net/http"
require "uri"
require "base64"

module NHSx
  module Export
    include BatchZipCreation
    include Gaudi::Utilities
    include Zuehlke::Package

    # Download server public key
    def download_server_public_key(server_key_arn, download_location, system_config)
      public_key = File.join(download_location, "public_key.der")
      run_command("Download server public key", NHSx::AWS::Commandlines.download_public_key(server_key_arn, public_key), system_config)
      return public_key
    end

    # Uploads dummy diagnosis keys (to force a new signing in aws)
    def upload_distribution_keys(diagnosis_key_submission_url, payload)
      url = URI(diagnosis_key_submission_url)

      https = Net::HTTP.new(url.host, url.port)
      https.use_ssl = true

      request = Net::HTTP::Post.new(url)
      request.body = payload

      puts "POST> #{url}"
      response = https.request(request)
      puts response.code
    end

    # Trigger the export batch lambda to update with latest keys
    def trigger_batch(batch_zip_lambda_name, system_config)
      output_file = NHSx::AWS::Commandlines.new_lambda_output_file(batch_zip_lambda_name, $configuration)
      run_command("Trigger export batch", NHSx::AWS::Commandlines.invoke_lambda(batch_zip_lambda_name, "", output_file), system_config)
    end

    # Verifies the signature in export_sig_path against a batch key archive in export_bin_path
    def verify_key_export(server_key_arn, export_bin_path, export_sig_path, system_config)
      export_sig = File.read(export_sig_path)
      signature_list = TEKSignatureList.decode(export_sig)
      raise GaudiError, "We expect a single signature in the list. Found #{signature_list.signatures.length}" if signature_list.signatures.length != 1

      bin_digest = "#{File.dirname(export_bin_path)}/export.bin.sha256"
      cmd = run_command("Create bin sha256 digest", "openssl dgst -sha256 -binary #{export_bin_path}", system_config)
      write_file(bin_digest, cmd.output.chomp)
      run_command("Aws verify digest signature", NHSx::AWS::Commandlines.verify_digest_signature(server_key_arn, bin_digest, export_sig_path), system_config)
    end

    # Bundles the previously downloaded export zip and server public key into a new zip
    def bundle_export_and_public_key(environment, download_location, system_config)
      bundle_zip = "#{environment}_env_export_bundle.zip"
      export_zip = "export.zip"
      public_key = "public_key_#{environment}.der"
      cmd = "cd #{download_location} && zip -r #{bundle_zip} #{export_zip} #{public_key} && cd -"
      run_command("Package key and example batch for #{environment}", cmd, system_config)
      puts "*" * 74
      puts "Created Apple & Google key and export bundle for #{environment} in #{bundle_zip}"
      bundle_zip
    end

    def generate_signature_info(bundle_id)
      signature_info = SignatureInfo.new
      signature_info.app_bundle_id = bundle_id
      signature_info.android_package = bundle_id
      signature_info.verification_key_version = "v1"
      signature_info.verification_key_id = "234"
      signature_info.signature_algorithm = "1.2.840.10045.4.3.2"
      signature_info
    end

    def generate_tek_export(upload_keys_json_path, signature_info)
      test_submission_keys_file = File.join($configuration.base, upload_keys_json_path)
      submission_keys_json = JSON.parse(File.read(test_submission_keys_file))

      temporary_exposure_key_export = TemporaryExposureKeyExport.new
      submission_keys_json["temporaryExposureKeys"].each { |key|
        temporary_exposure_key = TemporaryExposureKey.new
        temporary_exposure_key.key_data = ::Base64.decode64(key["key"])
        temporary_exposure_key.rolling_start_interval_number = key["rollingStartNumber"]
        temporary_exposure_key.rolling_period = key["rollingPeriod"]
        temporary_exposure_key.transmission_risk_level = 7
        temporary_exposure_key_export.keys << temporary_exposure_key
      }
      temporary_exposure_key_export.signature_infos << signature_info
      temporary_exposure_key_export
    end

    def generate_tek_signature_list(signature, signature_info)
      tek_signature = TEKSignature.new
      tek_signature.batch_num = 1
      tek_signature.batch_size = 1
      tek_signature.signature = signature
      tek_signature.signature_info = signature_info
      tek_signature_list = TEKSignatureList.new
      tek_signature_list.signatures << tek_signature
      tek_signature_list
    end

    def write_export_bin(export_bin_path, tek_export)
      serialized_bin = "EK Export v1    #{TemporaryExposureKeyExport.encode(tek_export)}"
      write_file(export_bin_path, serialized_bin)
    end

    def write_export_sig(export_sig_path, tek_signature_list)
      serialized_sig = TEKSignatureList.encode(tek_signature_list)
      write_file(export_sig_path, serialized_sig)
    end

    def aws_sign_export_bin(export_bin_path, export_bin_digest_path, server_key_arn)
      run_command("Create bin sha256 digest", "openssl dgst -sha256 -binary #{export_bin_path} > #{export_bin_digest_path}", $configuration)

      cmdline = NHSx::AWS::Commandlines.sign_digest_from(server_key_arn, export_bin_digest_path)
      cmd = run_command("Sign the export hash", cmdline, $configuration)
      aws_signature = cmd.output.chomp
      aws_signature
    end

    def download_public_key_as_pem(export_location, server_key_arn)
      public_key_path_der = download_server_public_key(server_key_arn, export_location, $configuration)
      convert_der_into_pem(public_key_path_der)
    end

    def convert_der_into_pem(public_key_path_der)
      public_key_pem = public_key_path_der.gsub(".der", ".pem")
      cmdline = "openssl ec -inform DER -outform PEM -in #{public_key_path_der} -out #{public_key_pem} -pubin"
      sh(cmdline)
      public_key_pem
    end

    def verify_signature(signature, export_bin_digest_path, server_key_arn)
      cmdline = NHSx::AWS::Commandlines.verify_digest_signature(server_key_arn, export_bin_digest_path, signature)
      run_command("Aws verify digest signature", cmdline, $configuration)
    end

    def create_bundle(bundle_name, export_bin_path, export_sig_path, public_key_pem_path, export_location)
      export_location_bundle = File.join(export_location, bundle_name)
      mkdir_p(export_location_bundle, :verbose => false)

      cp_r(export_bin_path, export_location_bundle, :verbose => false)
      cp_r(export_sig_path, export_location_bundle, :verbose => false)
      cp_r(public_key_pem_path, export_location_bundle, :verbose => false)
      create_package(export_location_bundle, $configuration)
    end

    def generate_bundle(export_location, server_key_arn, bundle_id, bundle_name)
      export_bin_path = File.join(export_location, "export.bin")
      export_sig_path = File.join(export_location, "export.sig")
      export_bin_digest_path = File.join(export_location, "export.bin.digest")

      signature_info = generate_signature_info(bundle_id)

      tek_export = generate_tek_export("test/robot/data/keys/PoCSmokeTest.json", signature_info)
      write_export_bin(export_bin_path, tek_export)

      aws_signature = aws_sign_export_bin(export_bin_path, export_bin_digest_path, server_key_arn)

      tek_signature_list = generate_tek_signature_list(aws_signature, signature_info)
      write_export_sig(export_sig_path, tek_signature_list)

      public_key_pem_path = download_public_key_as_pem(export_location, server_key_arn)

      bundle_location = create_bundle(bundle_name, export_bin_path, export_sig_path, public_key_pem_path, export_location)

      verify_signature(aws_signature, export_bin_digest_path, server_key_arn)
      return bundle_location
    end

    def decode_export(serialized_export)
      stripped_header = serialized_export.slice(16, serialized_export.size)
      TemporaryExposureKeyExport.decode(stripped_header)
    end
  end
end
