# Migrations for Backend 1.12

module Migrations
  # Methods that perform data and configuration migrations for the transition from Backend 1.11 to Backend 1.12
  module V12
    include NHSx::Secret

    def migrate_aae_key_and_cert_to_p12(service_name, consumer_name, system_config)
      secret_path = "/#{service_name}/#{consumer_name}"
      suffix = Digest::SHA1.hexdigest(secret_path)[0..4]

      certificate_dir = File.join(system_config.out, "/certificates")
      mkdir_p(certificate_dir, :verbose => false)

      cert_file_name = File.join(certificate_dir, "cert.pem")
      run_command("Download cert.pem", "aws secretsmanager get-secret-value --secret-id #{secret_path}/private-certificate-#{suffix} --query 'SecretString' --region #{NHSx::AWS::AWS_REGION} --output text > #{cert_file_name}", system_config)

      key_file_name = File.join(certificate_dir, "key.pem")
      run_command("Download key.pem", "aws secretsmanager get-secret-value --secret-id #{secret_path}/private-key-#{suffix} --query 'SecretString' --region #{NHSx::AWS::AWS_REGION} --output text > #{key_file_name}", system_config)

      password = run_command("Lookup key password", "aws secretsmanager get-secret-value --secret-id #{secret_path}/certificate-encryption-password-#{suffix} --region #{NHSx::AWS::AWS_REGION} --query 'SecretString' --output text", system_config).output.strip

      p12_file_name = File.join(certificate_dir, "cert.p12")
      run_quiet("Create cert.p12", "openssl pkcs12 -export -inkey #{key_file_name} -in #{cert_file_name} -out #{p12_file_name} -passin 'pass:#{password}' -passout 'pass:#{password}'", system_config)

      if "none" == run_command("Foo", "aws secretsmanager describe-secret --region #{NHSx::AWS::AWS_REGION} --secret-id #{secret_path}/private-certificate-pkcs12-#{suffix} || echo 'none'", system_config).output.strip
        run_command("Upload cert.p23 (create)", "aws secretsmanager create-secret --region #{NHSx::AWS::AWS_REGION} --name #{secret_path}/private-certificate-pkcs12-#{suffix} --secret-binary fileb://#{p12_file_name}", system_config)
      else
        run_command("Upload cert.p23 (update)", "aws secretsmanager put-secret-value --region #{NHSx::AWS::AWS_REGION} --secret-id #{secret_path}/private-certificate-pkcs12-#{suffix} --secret-binary fileb://#{p12_file_name}", system_config)
      end
    end
  end
end

namespace :migrate do
  namespace :v12 do
    NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
      tgt_envs.each do |tgt_env|
        desc "Creates the api payload from the existing csv raw file"
        task :"post_districts:#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::Publish
          include NHSx::Terraform
          target_config = target_environment_configuration(tgt_env, account, $configuration)
          terraform_configuration = File.join($configuration.base, "src/aws/accounts", account)
          select_workspace(tgt_env, terraform_configuration, $configuration)

          post_districts_out_dir = File.join($configuration.out, "gen/post_districts")
          key_name = "raw/risky-post-districts"
          distribution_store = "post_districts_distribution_store"
          object_name = "#{target_config[distribution_store]}/#{key_name}"
          local_target = File.join(post_districts_out_dir, key_name)
          run_command("Download current risk data of #{tgt_env} deployment", NHSx::AWS::Commandlines.download_from_s3(object_name, local_target), $configuration)

          require "csv"
          require "json"

          csv_content = CSV.read(local_target, :headers => true, :liberal_parsing => true)

          payload_data = {
            "postDistricts" => {},
            "localAuthorities" => {},
          }

          csv_content.each { |row|
            payload_data["postDistricts"][row["# postal_district_code"]] = {
              "riskIndicator" => row[" risk_indicator"].gsub("\"", "").strip,
              "tierIndicator" => row[" tier_indicator"].gsub("\"", "").strip,
            }
          }

          local_target_migrated = File.join(post_districts_out_dir, "migrate/api-payload")
          write_file(local_target_migrated, JSON.dump(payload_data))

          ENV["UPLOAD_DATA"] = local_target_migrated
          Rake::Task["upload:post_districts:#{tgt_env}"].invoke
        end
      end
    end

    desc "Converts existing client certificate (key & cert in:SecretsManager) to pkcs12 (out:SecretsManager) - dev"
    task :"aae_cert:dev" do
      include Migrations::V12
      service_name = "aae"
      consumer_name = "advanced_analytics"
      migrate_aae_key_and_cert_to_p12(service_name, consumer_name, $configuration)
    end

    desc "Converts existing client certificate (key & cert in:SecretsManager) to pkcs12 (out:SecretsManager) - staging"
    task :"aae_cert:staging" => [:"login:staging"] do
      include Migrations::V12
      service_name = "aae"
      consumer_name = "advanced_analytics"
      migrate_aae_key_and_cert_to_p12(service_name, consumer_name, $configuration)
    end

    desc "Converts existing client certificate (key & cert in:SecretsManager) to pkcs12 (out:SecretsManager) - prod"
    task :"aae_cert:prod" => [:"login:prod"] do
      include Migrations::V12
      service_name = "aae"
      consumer_name = "advanced_analytics"
      migrate_aae_key_and_cert_to_p12(service_name, consumer_name, $configuration)
    end
  end
end
