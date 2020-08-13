require "aws-sdk"

module NHSx
  # Helpers to run queries against DynamoDB
  module Query
    # Run a query
    def execute_dynamodb_query(query_params)
      dynamodb = Aws::DynamoDB::Client.new
      result = dynamodb.query(query_params)
      result.items
    rescue Aws::DynamoDB::Errors::ServiceError => e
      raise GaudiError, "Unable to query table:\n#{e.message}"
    end

    def scan_dynamodb(scan_params)
      dynamodb = Aws::DynamoDB::Client.new
      dynamodb.scan(scan_params)
    rescue Aws::DynamoDB::Errors::ServiceError => e
      raise GaudiError, "Unable to query table:\n#{e.message}"
    end

    def cta_token(token, target_config)
      table_name = target_config["virology_table_test_orders"]
      params = {
        :table_name => table_name,
        :key_condition_expression => "#tkn = :token_value",
        :expression_attribute_names => {
          "#tkn" => "ctaToken",
        },
        :expression_attribute_values => {
          ":token_value" => token,
        },
      }
      token_entries = execute_dynamodb_query(params)
      return {} if token_entries.empty?

      return token_entries.first
    end
  end
end
