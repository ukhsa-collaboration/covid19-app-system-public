require "faraday"

module NHSx
  # Wrappers for publishing data via the system's upload APIs
  module Upload
    # Upload the post district data from JBC
    #
    # Returns the response status
    def upload_post_district_data(upload_data, target_environment, target_config, system_config)
      authentication_token = target_config["auth_headers"]["highRiskPostCodeUpload"]
      headers = {
        "Content-Type" => "text/csv",
        "Authorization" => authentication_token,
      }
      if ["prod", "staging"].include?(target_environment)
        headers["x-custom-oai"] = system_config.custom_oai
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
