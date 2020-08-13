require_relative "../../../../protobuf/ruby/lib/exposure_pb"

module NHSx
  # Methods to automate grey box testing for the PoC
  module PoC
    include Zuehlke::Execution
    include BatchZipCreation

    API_ENDPOINT = "https://i397nak83k.execute-api.eu-west-2.amazonaws.com".freeze

    def sync_keys(system_config)
      run_command("Sync the uploaded data", "aws s3 sync s3://dev-api-dev-json-lambda-api-wlnoamhwrgqejvfp out/keys", system_config)
    end

    def count_keys(system_config)
      num_of_keys = 0
      Rake::FileList["#{system_config.out}/keys/*.json"].each do |payload|
        content = JSON.load(File.read(payload))
        num_of_keys += content.fetch("temporaryExposureKeys", {}).size
      end
      return num_of_keys
    end

    def fetch_export(system_config)
      cmdline = "curl -v --silent --show-error --fail -L \"#{API_ENDPOINT}/dev/v1/distribution/latest\" --output '#{system_config.out}/export.zip'"
      run_command("Fetch export archive", cmdline, system_config)
    end

    def unpack_export(system_config)
      Dir.chdir(system_config.out) do
        run_command("Unpack export archive", "unzip export.zip", system_config)
      end
    end

    def decode_signature(serialized_signature)
      TEKSignatureList.decode(serialized_signature)
    end

    def verify_export(system_config)
      unpack_export(system_config)
      Dir.chdir(system_config.out) do
        raise GaudiError, "Missing export.bin" unless File.exist?("export.bin")
        raise GaudiError, "Missing export.sig" unless File.exist?("export.sig")
      end
      signature = decode_signature(File.read(File.join(system_config.out, "export.sig")))
      export = decode_export(File.read(File.join(system_config.out, "export.bin")))
      return export.keys.size
    end
  end
end
