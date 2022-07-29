require 'zip'
require 'csv'

module NHSx
  module Virology
    include NHSx::AWS

    def order(url, target_env_config, system_config)
      authentication_token = target_env_config["auth_headers"]["mobile"]
      lambda_function = target_env_config["virology_submission_lambda_function_name"]
      custom_oai = NHSx::AWS::Commandlines.get_lambda_custom_oai(lambda_function, system_config)

      headers = {
        "Authorization" => authentication_token,
        "x-custom-oai" => custom_oai
      }

      resp = Faraday.post(url) do |req|
        req.headers = headers
      end

      raise GaudiError, "Expecting status code to be 200 but was #{resp.status}" unless resp.status == 200

      JSON.parse(resp.body)
    end

    def upload_result(url, payload, target_env_config, system_config)
      authentication_token = target_env_config["auth_headers"]["testResultUpload"]
      lambda_function = target_env_config["virology_upload_lambda_function_name"]
      custom_oai = NHSx::AWS::Commandlines.get_lambda_custom_oai(lambda_function, system_config)

      headers = {
        "Content-Type" => "application/json",
        "Authorization" => authentication_token,
        "x-custom-oai" => custom_oai
      }

      resp = Faraday.post(url) do |req|
        req.headers = headers
        req.body = JSON.dump(payload)
      end

      raise GaudiError, "Expecting status code to be 202 but was #{resp.status}" unless resp.status == 202
    end

    def result(url, payload, target_env_config, system_config)
      authentication_token = target_env_config["auth_headers"]["mobile"]
      lambda_function = target_env_config["virology_submission_lambda_function_name"]
      custom_oai = NHSx::AWS::Commandlines.get_lambda_custom_oai(lambda_function, system_config)

      headers = {
        "Content-Type" => "application/json",
        "Authorization" => authentication_token,
        "x-custom-oai" => custom_oai
      }

      resp = Faraday.post(url) do |req|
        req.headers = headers
        req.body = JSON.dump(payload)
      end

      if resp.status == 204
        return { "status" => "pending" }
      elsif resp.status == 200
        return JSON.parse(resp.body)
      else
        raise GaudiError, "Expecting status code to be 200 or 204 but was #{resp.status}"
      end
    end

    def token_gen(url, payload, target_env_config, system_config)
      authentication_token = target_env_config["auth_headers"]["testResultUpload"]
      lambda_function = target_env_config["virology_upload_lambda_function_name"]
      custom_oai = NHSx::AWS::Commandlines.get_lambda_custom_oai(lambda_function, system_config)

      headers = {
        "Content-Type" => "application/json",
        "Authorization" => authentication_token,
        "x-custom-oai" => custom_oai
      }

      resp = Faraday.post(url) do |req|
        req.headers = headers
        req.body = JSON.dump(payload)
      end

      raise GaudiError, "Expecting status code to be 200 but was #{resp.status}" unless resp.status == 200

      JSON.parse(resp.body)["ctaToken"]
    end

    def generate_single(config_file, system_config)
      _generate(
        target_env: config_file['target_environment_name'],
        test_kit: system_config.test_kit,
        test_result: system_config.test_result,
        test_end_date: system_config.test_end_date,
        number_of_tokens: system_config.number_of_tokens,
        lambda_function: config_file["virology_tokens_processing_function"],
        s3_bucket:config_file["virology_tokens_processing_output_store"],
        system_config: system_config,
      )
     end

    def generate_batch(config_file, system_config)
      # expect an env var INPUT_FILE set, which points to a CSV with headers:
      #     test_kit, test result, test end date, number_of_tokens
      input_file = system_config.input_file
      batches = CSV.open(
          input_file, "r",
          strip: true,
          headers: true, return_headers: true,
          header_converters: proc {|header| header.strip.to_sym },
      ) do |csv|
        header = csv.shift
        expected_headers = %w[test_kit test_result test_end_date number_of_tokens]
        if !header || header.fields.sort != expected_headers.sort
          raise GaudiError, "input csv does not have expected headers: #{expected_headers.join(', ')}"
        end

        csv.each do |row|
          _generate(
            target_env: config_file['target_environment_name'],
            lambda_function: config_file["virology_tokens_processing_function"],
            s3_bucket:config_file["virology_tokens_processing_output_store"],
            system_config: system_config,
            **row
          )
        end
      end
    end

    def _generate(
      target_env:,
      test_kit:,
      test_result:,
      test_end_date:,
      number_of_tokens:,
      lambda_function:,
      s3_bucket:,
      system_config:
    )
      # virology out dir (format: YYYYmmdd)
      date = Time.iso8601(test_end_date).strftime("%Y%m%d")
      virology_out_dir = File.join(system_config.out, "gen/virology/#{date}")

      # if virology exists for input test end date then raise
      output_file = File.join(
        virology_out_dir,
        "#{date}-#{target_env}-#{test_kit}-#{test_result}.csv".downcase
       )
       raise GaudiError, (
         "Local virology file for date: #{test_end_date} and target env:" +
         " #{target_env} and test kit: #{test_kit} already exists --" +
         " #{output_file}"
       ) unless !File.exist?(output_file)

      # lambda and payload
      payload = {
        "testResult" => test_result,
        "testEndDate" => test_end_date,
        "testKit" => test_kit,
        "numberOfTokens" => number_of_tokens
      }
      payload_json = "#{JSON.dump(payload)}"

      # blocking invoke lambda
      output_log_file = NHSx::AWS::Commandlines.new_lambda_output_file(lambda_function, system_config)
      cmd_line = NHSx::AWS::Commandlines.invoke_lambda(lambda_function, payload_json, output_log_file)
      cmd = run_command("Invoke virology lambda", cmd_line, system_config)
      puts cmd.output

      # get zip filename from response
      response_json = JSON.parse(File.read(output_log_file).gsub('\\"', '"'))
      puts JSON.pretty_generate(response_json)
      zip_filename = response_json["filename"]

      # download zip from s3 bucket
      object_name = "#{s3_bucket}/#{zip_filename}"
      zip_file_path = File.join(virology_out_dir, "#{zip_filename}")
      run_command("Download zip file", NHSx::AWS::Commandlines.download_from_s3(object_name, zip_file_path), system_config)

      # unzip to virology out dir
      run_command("Unzip archive", "unzip #{zip_file_path} -d #{virology_out_dir}", system_config)
      csv_filename = zip_filename.sub(".zip", ".csv")
      File.rename(File.join(virology_out_dir, "#{csv_filename}"), output_file)

      new_columns = [
        # we prepend a space to keep the formatting of the csv as is
        {'name' => ' environment', 'index' => 1, 'value' => ' ' + target_env.downcase},
        {'name' => ' test_kit', 'index' => 2, 'value' => ' ' + test_kit},
      ]
      append_columns_to_csv(output_file, new_columns)

      # remove downloaded s3 zip file
      File.delete(zip_file_path)
    end

    def generate_interval(config_file, system_config)
      virology_out_dir = File.join(system_config.out, "gen/virology/")

      # get TEST_RESULT and NUMBER_OF_TOKENS from env
      number_of_tokens = system_config.number_of_tokens
      test_result = system_config.test_result
      test_kit = system_config.test_kit

      # get START_DATE and NUMBER_OF_DAYS from env and validate
      start_date = Time.iso8601(system_config.start_date)
      number_of_days = system_config.number_of_days
      now = Time.now
      start_of_today = Time.new(now.year, now.month, now.day, 0, 0, 0, 0)
      raise GaudiError, "START_DATE must not be in the past" unless start_date >= start_of_today

      # generate command line parameters based from [START_DATE, START_DATE+NUMBER_OF_DAYS]
      one_day = 24 * 60 * 60
      virology_gen_args = (0..number_of_days - 1).map { |n|
        test_end_date_time = (start_date + n * one_day)
        local_test_end_date_dir = test_end_date_time.strftime("%Y%m%d")
        virology_single_day_dir = File.join(virology_out_dir, "#{local_test_end_date_dir}")
        raise GaudiError, "Local virology dir for #{local_test_end_date_dir} already exists #{virology_out_dir}" unless !File.exist?(virology_single_day_dir)

        {
          :test_kit => test_kit,
          :test_end_date => test_end_date_time.strftime('%Y-%m-%dT00:00:00Z'),
          :number_of_tokens => number_of_tokens,
          :test_result => test_result
        }
      }

      # display all the parameters that are going to be used to generate tokens
      require "highline"
      cli = HighLine.new
      puts "\nGenerating virology data with (please verify correctness):"
      virology_gen_args.each { |e| puts e }
      answer = cli.ask "\nDo you want to proceed? Type 'yes' to confirm"
      raise GaudiError, "Aborted" unless ["yes"].include?(answer.downcase)

      # generate tokens for each TEST_END_DATE
      virology_gen_args.each { |args|
        _generate(
          target_env: config_file['target_environment_name'],
          lambda_function: config_file["virology_tokens_processing_function"],
          s3_bucket:config_file["virology_tokens_processing_output_store"],
          system_config: system_config,
          **args
        )
      }

      # generate random pwd and store it
      zip_password = SecureRandom.alphanumeric(10)

      # zip csv with generated password
      start_date_formatted = start_date.strftime("%Y%m%d")
      end_date_day_formatted = (start_date + (number_of_days - 1) * one_day).day
      zip_file_name = "virology-#{start_date_formatted}-#{end_date_day_formatted}.zip"
      zip_enc_cmd = "cd #{virology_out_dir} && zip -r -e #{zip_file_name} * -P #{zip_password} && cd -"
      run_command("Zip archive password protected", zip_enc_cmd, system_config)

      # write pwd
      write_file(File.join(virology_out_dir, "pwd.txt"), zip_password)
    end

    def subscribe_mobile_number_to_topic(mobile_number,batch_number,config_file, system_config)
      # get the topic arn
      topic_arn = config_file["virology_tokens_processing_sms_topic_arn"]       

      #subscribe the mobile number   
      subscription_arn = subscribe_to_topic(topic_arn,"sms",mobile_number,system_config)

      # get the list of subscriptions
      sns_topic_subscriptions(topic_arn,system_config)
      
      # Apply filter policy    
      apply_filter_policy(subscription_arn,batch_number,mobile_number,system_config)

      # Get subscription attributes to verify the filter policy
      get_subscription_attributes(subscription_arn,system_config)      
    end  

    def subscribe_email_to_topic(email,batch_number,config_file, system_config)
      # get the topic arn
      topic_arn = config_file["virology_tokens_processing_email_topic_arn"]       

      #subscribe the email     
      subscription_arn = subscribe_to_topic(topic_arn,"email",email,system_config)

      # get the list of subscriptions
      sns_topic_subscriptions(topic_arn,system_config)
      
      # Apply filter policy     
      apply_filter_policy(subscription_arn,batch_number,email,system_config)

      # Get subscription attributes to verify the filter policy
      get_subscription_attributes(subscription_arn,system_config)     
    end

    def subscribe_to_topic(topic_arn,protocol,endpoint,system_config)
      cmdline = "aws sns subscribe --topic-arn #{topic_arn} --protocol #{protocol} --return-subscription-arn --notification-endpoint #{endpoint}"
      cmd = run_command("Subscribing #{endpoint}", cmdline, system_config)      
      subscription_arn = JSON.parse(cmd.output)["SubscriptionArn"]
      return subscription_arn
    end  

    def sns_topic_subscriptions(topic_arn,system_config)
      cmdline = "aws sns list-subscriptions-by-topic --topic-arn #{topic_arn}"
      cmd = run_command("Getting the list of Subscriptions", cmdline, system_config)
      subscriptions = JSON.parse(cmd.output)["Subscriptions"]
      return subscriptions
    end       

    def apply_filter_policy(subscription_arn,batch_number,mobile_number,system_config)
      #build the input parameters      
      input_parameters = {
        "SubscriptionArn" => subscription_arn,
        "AttributeName" => "FilterPolicy",       
        "AttributeValue" => "{\"batchNumber\":[{\"numeric\": [\"=\",#{batch_number}]}]}",
      }
      input_config_file = File.join(system_config.out, "gen/virology", "#{Time.now.strftime("%Y%m%d%H%M%S")}_filter_policy_#{batch_number}.json")
      write_file(input_config_file,  JSON.pretty_generate(input_parameters))
      
      cmdline = "aws sns set-subscription-attributes --cli-input-json file://#{input_config_file}"
      cmd = run_command("Applying filter policy for #{mobile_number}", cmdline, system_config)
    end

    def get_subscription_attributes(subscription_arn,system_config)
      cmdline = "aws sns get-subscription-attributes --subscription-arn #{subscription_arn}"
      cmd = run_command("Getting the list of attributes to check the filter policy ", cmdline, system_config)      
      attributes = JSON.parse(cmd.output)["Attributes"]
      return attributes
    end

    def append_columns_to_csv(output_file, new_columns)
      # prefetch csv to prevent concurrent reading and writing
      in_csv_arr = CSV.open(
        output_file, "r",
        headers: true, return_headers: true,
      ).to_a

      CSV.open(
        output_file, "w",
        headers: true, write_headers: true,
      ) do |csv|
        in_csv_arr.each do |row|
          csv << append_columns_to_row(row, new_columns)
        end
      end
    end

    def append_columns_to_row(row, new_columns)
      new_row = row.fields()
      new_columns.each do |column|
        if row.header_row?
          value = column["name"]
        else
          value = column["value"]
        end
        new_row.insert(column["index"], value)
      end
      return new_row
    end
  end
end
