require 'aws-sdk-sqs'

module NHSx

  module SQS
    def move_and_delete_all_sqs_events(src_queue_name, dst_queue_name)
      sqs = Aws::SQS::Client.new
      puts "Sending all available sqs events from #{src_queue_name} queue to #{dst_queue_name} queue"
      source_queue_url = get_queue_url(sqs, src_queue_name)
      messages = get_messages(10, source_queue_url, sqs)
      while messages.length() > 0 do
        move_messages(sqs, messages, dst_queue_name, src_queue_name, source_queue_url)
        messages = get_messages(10, source_queue_url, sqs)
      end

    end

    def get_messages(max_no_of_messages, source_queue_url, sqs)
      params= {
          :queue_url => source_queue_url,
          :message_attribute_names => ["All"],
          :max_number_of_messages => max_no_of_messages,
          :wait_time_seconds => 0
      }
      receive_resp = sqs.receive_message(params)
      receive_resp.messages
    end

    def move_messages(sqs, messages, dst_queue_name, src_queue_name, source_queue_url)
      destination_queue_url = get_queue_url(sqs, dst_queue_name)
      messages.each do |message|
        sqs.send_message({:queue_url => destination_queue_url,
                          :message_body => message.body,
                          :message_attributes => message.message_attributes})
        puts "Sent message #{message.message_id} from queue #{src_queue_name} to #{dst_queue_name}"
        sqs.delete_message({:queue_url => source_queue_url,
                            :receipt_handle => message.receipt_handle})
        puts "Deleted message #{message.message_id} from queue #{src_queue_name}"
      end
    end

    def get_queue_url(sqs, queue_name)
      begin
        sqs.get_queue_url(queue_name: queue_name).queue_url
      rescue Aws::SQS::Errors::NonExistentQueue
        raise GaudiError, "A queue named '#{queue_name}' does not exist."
      end
    end

  end
end