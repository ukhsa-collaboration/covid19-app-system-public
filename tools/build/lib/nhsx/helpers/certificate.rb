require_relative 'secret'

module NHSx
  # Helper methods for creating api keys
  module Certificate
    include NHSx::Secret

    def download_aae_secrets(aae_config, system_config)
      download_secrets(aae_config['service_name'], aae_config['consumer_name'], aae_config, system_config)
    end

    def store_aae_certificates(aae_config, system_config)
      store_aae_x509_certificates(aae_config['service_name'], aae_config['consumer_name'], aae_config, system_config)
      store_aae_pkcs12_certificates(aae_config['service_name'], aae_config['consumer_name'], aae_config, system_config)

      print_aae_certificate_issuer(aae_config, system_config)
      print_aae_certificate_fingerprint(aae_config, system_config)
      puts '*' * 74
      puts 'AWS Account User needs to share the public key with AAE Environment'
      puts aae_config['public_key_path']
      puts '*' * 74

      puts 'Current Certificate Secrets Stored'
      secret_manager_arns = list_aae_advanced_analytics_secrets(aae_config['service_name'], aae_config['consumer_name'], system_config)
      puts secret_manager_arns
    end

    def create_aae_certificates(aae_config, system_config)
      create_certificate_dir(aae_config)
      create_aae_x509_certificate(aae_config, system_config)
      create_aae_pkcs12_certificate(aae_config, system_config)

      puts 'Created Certificate Details'
      print_aae_certificate_issuer(aae_config, system_config)
      print_aae_certificate_fingerprint(aae_config, system_config)

      puts '*' * 74
      puts 'AWS Account User needs to share the public key with AAE Environment'
      puts aae_config['public_key_path']
      puts '*' * 74
    end

    def create_aae_pkcs12_certificate(aae_config, system_config)
      certificate_path = aae_config['certificate_path']
      certificate_key_path = aae_config['certificate_key_path']
      certificate_pkcs12_path = aae_config['certificate_pkcs12_path']

      password_path = aae_config['password_path']
      password = read_file_content_in(password_path)

      cmdline = "openssl pkcs12 -export -inkey #{certificate_key_path} -in #{certificate_path} -out #{certificate_pkcs12_path} -passin 'pass:#{password}' -passout 'pass:#{password}'"
      run_quiet('Create the AAE pkcs12 certificate', cmdline, system_config)
    end

    def create_aae_x509_certificate(aae_config, system_config)
      certificate_key_path = aae_config['certificate_key_path']
      certificate_path = aae_config['certificate_path']
      public_key_path = aae_config['public_key_path']
      password = aae_config['password']
      escaped_password = Shellwords.escape(password)

      subject = "'/C=GB/ST=London/L=London/O=NHS/OU=Test and Trace/CN=svc-test-trace.nhs.uk/emailAddress=one.email@nhsx.nhs.uk'"
      cmdline = "openssl req -x509 -newkey rsa:4096 -days 365 -outform PEM \
      -keyout #{certificate_key_path} \
      -out #{certificate_path} \
      -passin pass:#{escaped_password} \
      -passout pass:#{escaped_password} \
      -subj #{subject}"
      run_quiet('Create the AAE Private certificate (x509)', cmdline, system_config)

      cmdline = "openssl x509 -outform der -in #{certificate_path} -out #{public_key_path}"
      run_command('Create the AAE Public certificate (x509)', cmdline, system_config)
      run_quiet('Write Password String', "echo #{escaped_password} > #{aae_config['password_path']}", system_config)
    end

    def create_certificate_dir(aae_config)
      certificate_dir = aae_config['certificate_dir']
      rm_rf(certificate_dir, verbose: false)
      mkdir_p(certificate_dir, verbose: false)
    end

    def print_aae_certificate_issuer(aae_config, system_config)
      cmdline = "openssl x509 -in #{aae_config["certificate_path"]} -issuer -noout"
      cmd = run_command("Print the certificate's issuer", cmdline, system_config)
      puts cmd.output
    end

    def print_aae_certificate_fingerprint(aae_config, system_config)
      cmdline = "openssl x509 -in #{aae_config["certificate_path"]} -fingerprint -noout"
      cmd = run_command("Print the certificate's fingerprint", cmdline, system_config)
      puts cmd.output
    end

    def clean_aae_certificate(aae_config)
      rm_rf(aae_config['certificate_path'], verbose: true)
      rm_rf(aae_config['certificate_key_path'], verbose: true)
      rm_rf(aae_config['certificate_pkcs12_path'], verbose: true)
      rm_rf(aae_config['password_path'], verbose: true)

      puts 'Clean up complete'
    end
  end
end
