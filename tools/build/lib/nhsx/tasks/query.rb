require "json"
namespace :query do
  include NHSx::TargetEnvironment
  include NHSx::Terraform
  include NHSx::Query

  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      desc "Query a ctaToken (defined in TOKEN) from the test results database in #{tgt_env}"
      task :"ctatoken:#{tgt_env}" => [:"login:#{account}"] do
        include NHSx::Generate
        include NHSx::Query

        token = $configuration.token

        target_config = JSON.parse(File.read(generate_test_config(tgt_env, account, $configuration)))

        token_data = cta_token(token, target_config)
        puts "*" * 80
        if token_data.empty?
          puts "ctaToken #{token} is not in the system"
        else
          table_name = target_config["virology_table_results"]
          params = {
            :table_name => table_name,
            :key_condition_expression => "#tkn = :token_value",
            :expression_attribute_names => {
              "#tkn" => "testResultPollingToken",
            },
            :expression_attribute_values => {
              ":token_value" => token_data["testResultPollingToken"],
            },
          }
          test_result_entries = execute_dynamodb_query(params)
          puts "No test result entry for ctaToken #{token} found" if test_result_entries.empty?
          test_result_entry = test_result_entries.first
          puts "ctaToken #{token} is #{test_result_entry["status"]}"
          puts "Polling token is #{token_data["testResultPollingToken"]}"
          puts "result is #{test_result_entry["testResult"]}" if test_result_entry["status"] == "available"
          puts "testKit is #{test_result_entry["testKit"]}"
        end
        puts "queried at #{Time.now.strftime("%H:%M:%S %d/%m/%Y")}"
        puts "*" * 80
      end

      task :"pollingtoken:#{tgt_env}" => [:"login:#{account}"] do
        include NHSx::Generate
        include NHSx::Query

        token = $configuration.token

        target_config = JSON.parse(File.read(generate_test_config(tgt_env, account, $configuration)))

        table_name = target_config["virology_table_results"]
        params = {
          :table_name => table_name,
          :key_condition_expression => "#tkn = :token_value",
          :expression_attribute_names => {
            "#tkn" => "testResultPollingToken",
          },
          :expression_attribute_values => {
            ":token_value" => token,
          },
        }
        token_entries = execute_dynamodb_query(params)
        puts "*" * 80
        if token_entries.empty?
          puts "Polling token #{token} is not in the system"
        else
          token_data = token_entries.first

          table_name = target_config["virology_table_test_orders"]
          params = {
            :expression_attribute_names => {
              "#ctaToken" => "ctaToken",
              "#pollingToken" => "testResultPollingToken",
            },
            :expression_attribute_values => {
              ":pollingToken" => token,
            },
            :filter_expression => "testResultPollingToken = :pollingToken",
            :projection_expression => "#ctaToken, #pollingToken",
            :table_name => table_name,
          }

          test_result_entries = scan_dynamodb(params).items
          puts "No test result entry for #{token} corresponding to a ctaToken found" if test_result_entries.empty?
          test_result_entry = test_result_entries.first
          puts "Polling token corresponds to ctaToken #{test_result_entry["ctaToken"]}"
        end
        puts "queried at #{Time.now.strftime("%H:%M:%S %d/%m/%Y")}"
        puts "*" * 80
      end
    end
  end

  NHSx::TargetEnvironment::ANALYTICS_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      task :"athena:#{tgt_env}" => [:"login:#{account}"] do
        target_config = analytics_target_environment_configuration(tgt_env, account, $configuration)
        query_id = target_config["sip_analytics_create_daily_aggregate_table_query_id"]
        database_name = target_config["sip_analytics_database"]
        workgroup_name = target_config["analytics_workgroup"]

        raise GaudiError, "Missing value for query in sip_analytics_create_daily_aggregate_table_query_id" unless query_id

        queries = [{
          "query_id" => query_id,
          "database" => database_name,
          "workgroup" => workgroup_name,
        }]

        queries.each do |q|
          query = get_athena_named_query(q["query_id"])
          start_athena_query(q["query_id"], query, q["database"], q["workgroup"])
        end
      end
    end
  end
end
