namespace :set do
  NHSx::TargetEnvironment::TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      desc "Update the result of a ctaToken (define in TOKEN) to positive"
      task :"token:positive:#{tgt_env}" => [:"login:#{account}"] do
        include NHSx::Query
        include NHSx::Update
        include NHSx::Generate

        token = $configuration.token

        target_config = JSON.parse(File.read(generate_test_config(tgt_env, account, $configuration)))

        token_data = cta_token(token, target_config)

        if token_data.empty?
          puts "ctaToken #{token} is not in the system"
        else
          table_name = target_config["virology_table_results"]
          params = {
            :table_name => table_name,
            :key => {
              :testResultPollingToken => token_data["testResultPollingToken"],
            },
            :update_expression => "set testResult = :r, testEndDate = :d, #s = :a",
            :expression_attribute_names => {
              "#s" => "status",
            },
            :expression_attribute_values => {
              ":r" => "POSITIVE",
              ":d" => (Time.now.utc + (60 * 60 * 48)).strftime("%Y-%m-%dT00:00:00Z"),
              ":a" => "available",
            },
            :return_values => "UPDATED_NEW",
          }

          update_dynamodb_entry(params)
          puts "Updated test result corresponding to #{token}"

          submission_token = token_data["diagnosisKeySubmissionToken"]
          table_name = target_config["virology_table_submission_tokens"]
          item = { :diagnosisKeySubmissionToken => submission_token }
          params = {
            :table_name => table_name,
            :item => item,
          }
          create_dynamodb_entry(params)
          puts "Updated submission token #{submission_token} for #{token}"
          puts "Updated #{token} to POSITIVE"
        end
      end
      desc "Update the result of a ctaToken (define in TOKEN) to negative"
      task :"token:negative:#{tgt_env}" => [:"login:#{account}"] do
        include NHSx::Query
        include NHSx::Update
        include NHSx::Generate

        token = $configuration.token

        target_config = JSON.parse(File.read(generate_test_config(tgt_env, account, $configuration)))

        token_data = cta_token(token, target_config)

        if token_data.empty?
          puts "ctaToken #{token} is not in the system"
        else
          table_name = target_config["virology_table_results"]
          params = {
            :table_name => table_name,
            :key => {
              :testResultPollingToken => token_data["testResultPollingToken"],
            },
            :update_expression => "set testResult = :r, testEndDate = :d, #s = :a",
            :expression_attribute_names => {
              "#s" => "status",
            },
            :expression_attribute_values => {
              ":r" => "NEGATIVE",
              ":d" => (Time.now.utc + (60 * 60 * 48)).strftime("%Y-%m-%dT00:00:00Z"),
              ":a" => "available",
            },
            :return_values => "UPDATED_NEW",
          }

          update_dynamodb_entry(params)
          puts "Updated test result corresponding to #{token}"

          submission_token = token_data["diagnosisKeySubmissionToken"]
          table_name = target_config["virology_table_submission_tokens"]
          item = { :diagnosisKeySubmissionToken => submission_token }
          params = {
            :table_name => table_name,
            :item => item,
          }
          create_dynamodb_entry(params)
          puts "Updated submission token #{submission_token} for #{token}"
          puts "Updated #{token} to NEGATIVE"
        end
      end
    end
  end
end
