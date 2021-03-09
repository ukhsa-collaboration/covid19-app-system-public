require_relative "../aws"

module NHSx
  # Methods and modules for operating the CTA system's APIs
  module API
    # Finds the correct authentication key for the API and sets the default content type to json
    def self.determine_api_headers(api_key_name, lambda_function, target_environment_config, system_config)
      authentication_token = target_environment_config["auth_headers"][api_key_name]
      target_environment = target_environment_config["target_environment_name"]
      lambda_function_name = target_environment_config[lambda_function]

      raise GaudiError, "No target environment configuration entry for #{lambda_function}" unless lambda_function_name

      headers = {
        "Content-Type" => "application/json",
        "Authorization" => authentication_token,
      }

      headers["x-custom-oai"] = NHSx::AWS::Commandlines.get_lambda_custom_oai(lambda_function_name, system_config) if ["prod", "staging"].include?(target_environment)
      return headers
    end
    # Methods for API calls against the submission CF facade
    module Submission
      def create_isolation_payment_token(country, target_environment_config, system_config)
        target_environment = target_environment_config["target_environment_name"]
        valid_countries = ["England", "Wales"]
        raise GaudiError, "Invalid country specification #{country}. Valid choices are #{valid_countries.join(",")}" unless valid_countries.include?(country)

        headers = NHSx::API.determine_api_headers("mobile", "isolation_payment_order_lambda_function_name", target_environment_config, system_config)

        if ["prod", "staging"].include?(target_environment)
          url = target_environment_config["isolation_payment_create_gateway_endpoint"]
        else
          url = target_environment_config["isolation_payment_create_endpoint"]
        end

        payload = {
          "country" => country,
        }

        resp = Faraday.post(url) do |req|
          req.headers = headers
          req.body = JSON.dump(payload)
        end

        raise GaudiError, "Failed to create ipc token. Response returned #{resp.status}" unless resp.success?

        result = JSON.parse(resp.body)

        return result["ipcToken"]
      end

      def update_isolation_payment_token(ipc_token, target_environment_config, system_config)
        target_environment = target_environment_config["target_environment_name"]
        headers = NHSx::API.determine_api_headers("mobile", "isolation_payment_order_lambda_function_name", target_environment_config, system_config)

        if ["prod", "staging"].include?(target_environment)
          url = target_environment_config["isolation_payment_update_gateway_endpoint"]
        else
          url = target_environment_config["isolation_payment_update_endpoint"]
        end

        risky_encounter_date = Time.now.utc
        isolation_period_date = Time.now.utc + (60 * 60 * 48)
        payload = {
          "ipcToken" => ipc_token,
          "riskyEncounterDate" => risky_encounter_date.strftime("%Y-%m-%dT00:00:00Z"),
          "isolationPeriodEndDate" => isolation_period_date.strftime("%Y-%m-%dT00:00:00Z"),
        }

        resp = Faraday.post(url) do |req|
          req.headers = headers
          req.body = JSON.dump(payload)
        end

        raise GaudiError, "Failed to update ipc token. Response returned #{resp.status}" unless resp.success?

        return ipc_token
      end
    end
  end
end
