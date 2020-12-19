require 'aws-sdk-sqs'

module NHSx

  module SQS
    def move_and_delete_sqs_event(src_queue_name, dst_queue_name)
      sqs = Aws::SQS::Client.new
      puts "Sending sqs event from #{src_queue_name} queue to #{dst_queue_name} queue"
      begin
        source_queue_url = sqs.get_queue_url(queue_name: src_queue_name).queue_url
        receive_resp = sqs.receive_message({queue_url: source_queue_url,
                                            message_attribute_names: ["All"],
                                            max_number_of_messages: 1,
                                            wait_time_seconds: 0})
      rescue Aws::SQS::Errors::NonExistentQueue
        puts "A queue named '#{src_queue_name}' does not exist."
        exit(false)
      end

      if receive_resp.messages.empty?
        puts "Queue named '#{src_queue_name}' is empty."
      else
        begin
          destination_queue_url = sqs.get_queue_url(queue_name: dst_queue_name).queue_url
          receive_resp.messages.each do |message|
            sqs.send_message({queue_url: destination_queue_url,
                              message_body: message.body,
                              message_attributes: message.message_attributes})
            puts "Sent message #{message.message_id} from queue #{src_queue_name} to #{dst_queue_name}"
            sqs.delete_message({queue_url: source_queue_url,
                                receipt_handle: message.receipt_handle})
            puts "Deleted message #{message.message_id} from queue #{src_queue_name}"
          end
        rescue Aws::SQS::Errors::NonExistentQueue
          puts "A queue named '#{dst_queue_name}' does not exist."
          exit(false)
        end
      end
    end
  end
end