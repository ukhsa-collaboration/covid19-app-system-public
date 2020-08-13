module NHSx
  # Helpers to create/update entries in DynamoDB
  module Update
    def update_dynamodb_entry(update_params)
      dynamodb = Aws::DynamoDB::Client.new
      dynamodb.update_item(update_params)
    rescue Aws::DynamoDB::Errors::ServiceError => e
      raise GaudiError, "Unable to update table:\n#{e.message}"
    end

    def create_dynamodb_entry(create_params)
      dynamodb = Aws::DynamoDB::Client.new
      dynamodb.put_item(create_params)
    rescue Aws::DynamoDB::Errors::ServiceError => e
      raise GaudiError, "Unable to create entry:\n#{e.message}"
    end
  end
end
