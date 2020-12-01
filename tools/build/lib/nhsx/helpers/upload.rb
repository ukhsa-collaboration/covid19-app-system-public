require "faraday"

module NHSx
  # Wrappers for publishing data via the system's upload APIs
  module Upload
    # Upload the post district data from JBC
    #
    # Returns the response status
    def upload_post_district_data(upload_data, target_environment, target_config, system_config)
      lambda_function = target_config["risky_post_districts_upload_lambda_function_name"]

      authentication_token = target_config["auth_headers"]["highRiskPostCodeUpload"]
      custom_oai = NHSx::AWS::Commandlines.get_lambda_custom_oai(lambda_function, system_config)

      headers = {
          "Content-Type" => "text/csv",
          "Authorization" => authentication_token,
          "x-custom-oai" => custom_oai
      }

      if ["prod", "staging"].include?(target_environment)
        url = "#{target_config["risky_post_districts_upload_gateway_endpoint"]}/upload/high-risk-postal-districts"
      else
        url = target_config["risky_post_districts_upload_endpoint"]
      end

      resp = Faraday.post(url) do |req|
        req.headers = headers
        req.body = upload_data
      end

      return resp.status
    end
  end
end
