require "faraday"

namespace :virology do
  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      namespace :v2 do
        desc "Order and upload virology result v2 for TEST_KIT, TEST_RESULT"
        task :"order_upload:#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::Virology
          include NHSx::Generate
          test_kit = $configuration.test_kit
          test_result = $configuration.test_result

          target_config = target_environment_configuration(tgt_env, account, $configuration)
          url = "#{target_config["virology_kit_submission_gateway_endpoint"]}/virology-test/v2/order"

          order_response = order(url, target_config, $configuration)
          puts JSON.pretty_generate(order_response)

          cta_token = order_response["tokenParameterValue"]
          payload = {
            "ctaToken" => cta_token,
            "testEndDate" => (Time.now.utc + (60 * 60 * 48)).strftime("%Y-%m-%dT00:00:00Z"),
            "testResult" => test_result,
            "testKit" => test_kit,
          }
          url = "#{target_config["test_results_upload_gateway_endpoint"]}/upload/virology-test/v2/npex-result"

          upload_result(url, payload, target_config, $configuration)

          puts "Virology cta token #{cta_token} marked as #{test_result}/#{test_kit}"
        end
        desc "Order virology result v2"
        task :"order:#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::Virology
          include NHSx::Generate

          target_config = target_environment_configuration(tgt_env, account, $configuration)
          url = "#{target_config["virology_kit_submission_gateway_endpoint"]}/virology-test/v2/order"

          order_response = order(url, target_config, $configuration)
          puts JSON.pretty_generate(order_response)
          puts "Virology cta token #{order_response["tokenParameterValue"]} order completed"
        end
        desc "Upload a lab test result via upload api for TEST_KIT, TOKEN, TEST_RESULT"
        task :"upload:#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::Virology
          include NHSx::Generate

          test_kit = $configuration.test_kit
          cta_token = $configuration.cta_token
          test_result = $configuration.test_result

          target_config = target_environment_configuration(tgt_env, account, $configuration)

          payload = {
            "ctaToken" => cta_token,
            "testEndDate" => (Time.now.utc + (60 * 60 * 48)).strftime("%Y-%m-%dT00:00:00Z"),
            "testResult" => test_result,
            "testKit" => test_kit,
          }
          url = "#{target_config["test_results_upload_gateway_endpoint"]}/upload/virology-test/v2/npex-result"

          upload_result(url, payload, target_config, $configuration)

          puts "Virology cta token #{cta_token} marked as #{test_result}/#{test_kit}"
        end
        desc "Poll virology result v2 for POLLING_TOKEN"
        task :"poll_result:#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::Virology
          include NHSx::Generate
          polling_token = $configuration.polling_token

          target_config = target_environment_configuration(tgt_env, account, $configuration)
          url = "#{target_config["virology_kit_submission_gateway_endpoint"]}/virology-test/v2/results"

          payload = {
            "testResultPollingToken" => polling_token,
            "country" => "England"
          }

          puts JSON.dump(payload)

          poll_result_response = result(url, payload, target_config, $configuration)
          puts JSON.pretty_generate(poll_result_response)
          puts "Virology result polling completed"
        end
        desc "Token gen v2 for TEST_KIT"
        task :"token_gen:#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::Virology
          include NHSx::Generate

          test_kit = $configuration.test_kit
          test_result = $configuration.test_result

          target_config = target_environment_configuration(tgt_env, account, $configuration)

          payload = {
            "testEndDate" => (Time.now.utc + (60 * 60 * 48)).strftime("%Y-%m-%dT00:00:00Z"),
            "testResult" => test_result,
            "testKit" => test_kit,
          }
          url = "#{target_config["test_results_upload_gateway_endpoint"]}/upload/virology-test/v2/eng-result-tokengen"

          cta_token = token_gen(url, payload, target_config, $configuration)

          puts "Virology cta token #{cta_token} marked as #{test_result}/#{test_kit}"
        end
      end
      namespace :v1 do
        desc "Order and upload virology result v1 for TEST_KIT as LAB_RESULT"
        task :"order_upload:#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::Virology
          include NHSx::Generate
          test_kit = $configuration.test_kit

          target_config = target_environment_configuration(tgt_env, account, $configuration)
          url = "#{target_config["virology_kit_submission_gateway_endpoint"]}/virology-test/home-kit/order"

          order_response = order(url, target_config, $configuration)
          puts JSON.pretty_generate(order_response)

          cta_token = order_response["tokenParameterValue"]
          test_result = "POSITIVE"
          payload = {
            "ctaToken" => cta_token,
            "testEndDate" => (Time.now.utc + (60 * 60 * 48)).strftime("%Y-%m-%dT00:00:00Z"),
            "testResult" => test_result,
          }
          url = "#{target_config["test_results_upload_gateway_endpoint"]}/upload/virology-test/npex-result"

          upload_result(url, payload, target_config, $configuration)

          puts "Virology cta token #{cta_token} marked as #{test_result}/#{test_kit}"
        end
        desc "Token gen v1"
        task :"token_gen:#{tgt_env}" => [:"login:#{account}"] do
          include NHSx::Virology
          include NHSx::Generate
          test_kit = $configuration.test_kit

          target_config = target_environment_configuration(tgt_env, account, $configuration)

          test_result = "POSITIVE"
          payload = {
            "testEndDate" => (Time.now.utc + (60 * 60 * 48)).strftime("%Y-%m-%dT00:00:00Z"),
            "testResult" => test_result,
          }
          url = "#{target_config["test_results_upload_gateway_endpoint"]}/upload/virology-test/eng-result-tokengen"

          cta_token = token_gen(url, payload, target_config, $configuration)

          puts "Virology cta token #{cta_token} marked as #{test_result}/#{test_kit}"
        end
      end
    end
  end
end
