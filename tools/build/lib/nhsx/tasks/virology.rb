namespace :virology do
  NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      desc "Invoke virology token generator lambda with TEST_END_DATE, TEST_RESULT and NUMBER_OF_TOKENS"
      task :"gen:#{tgt_env}" => [:"login:#{account}"] do
        require 'zip'
        include NHSx::AWS
        include NHSx::Generate

        # virology out dir (format: YYYYmmdd)
        date = Time.iso8601($configuration.test_end_date).strftime("%Y%m%d")
        virology_out_dir = File.join($configuration.out, "gen/virology/#{date}")

        # if virology exists for input test end date then raise
        raise GaudiError, "Local virology dir for #{$configuration.test_end_date} already exists #{virology_out_dir}" unless !File.exist?(virology_out_dir)

        # target env config file
        config_file = JSON.parse(File.read(generate_test_config(tgt_env, account, $configuration)))

        # lambda and payload
        lambda_function = config_file["virology_tokens_processing_function"]
        payload = {
            "testResult" => $configuration.test_result,
            "testEndDate" => $configuration.test_end_date,
            "numberOfTokens" => $configuration.number_of_tokens
        }
        payload_json = "'#{JSON.dump(payload)}'"

        # blocking invoke lambda
        output_log_file = NHSx::AWS::Commandlines.new_lambda_output_file(lambda_function, $configuration)
        cmd_line = NHSx::AWS::Commandlines.invoke_lambda(lambda_function, payload_json, output_log_file)
        cmd = run_command("Invoke virology lambda", cmd_line, $configuration)
        puts cmd.output

        # get zip filename from response
        response_json = JSON.parse(File.read(output_log_file).gsub('\\"', '"')[1..-2])
        puts JSON.pretty_generate(response_json)
        zip_filename = response_json["filename"]

        # download zip from s3 bucket
        s3_bucket = config_file["virology_tokens_processing_output_store"]
        object_name = "#{s3_bucket}/#{zip_filename}"
        zip_file_path = File.join(virology_out_dir, "#{zip_filename}")
        run_command("Download zip file", NHSx::AWS::Commandlines.download_from_s3(object_name, zip_file_path), $configuration)

        # unzip to virology out dir
        run_command("Unzip archive", "unzip #{zip_file_path} -d #{virology_out_dir}", $configuration)
        csv_filename = zip_filename.sub(".zip", ".csv")
        File.rename(File.join(virology_out_dir, "#{csv_filename}"), File.join(virology_out_dir, "#{date}.csv"))

        # generate random pwd and store it
        zip_password = SecureRandom.alphanumeric(10)
        write_file(File.join(virology_out_dir, "#{date}.txt"), zip_password)

        # zip csv with generated password
        zip_enc_cmd = "cd #{virology_out_dir} && zip -e #{date}.zip #{date}.csv -P #{zip_password} && cd -"
        run_command("Zip archive password protected", zip_enc_cmd, $configuration)

        # remove downloaded s3 zip file
        File.delete(zip_file_path)
      end
    end
  end
end
