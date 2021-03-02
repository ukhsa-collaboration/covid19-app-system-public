require "json"

module NHSx
  # Helpers that codify the use of the AWS CLI for synthetic canaries within the NHSx project
  module AWS_Synth
    include NHSx::AWS
    # AWS CLI command lines in use by automation scripts
    module Commandlines
      def self.list_synth_canaries(region)
        "aws synthetics describe-canaries --region #{region}"
      end
    end # of module Commandlines

    def list_active_synth_engine_ids(region, system_config)
      cmdline = NHSx::AWS_Synth::Commandlines.list_synth_canaries(region)
      cmd = run_command("Retrieve list of existing synthetic canaries", cmdline, system_config)
      JSON.parse(cmd.output)["Canaries"].map {
              |el| el["EngineArn"].match(/:(cwsyn-[^:]*):\d+$/).captures[0] if el["EngineArn"].match(/:(cwsyn-[^:]*):\d+$/) }.compact
    end

    def get_all_synth_lambda_functions(region, system_config)
      cmdline = NHSx::AWS::Commandlines.list_lambda_functions(region)
      cmd = run_command("Retrieve list of all lambda functions", cmdline, system_config)
      JSON.parse(cmd.output)["Functions"].map {
              |el| el["FunctionArn"].match(/:(cwsyn-[^:]*)$/).captures[0] if el["FunctionArn"].match(/:(cwsyn-[^:]*)$/) }.compact
    end

    def get_all_synth_log_groups(region, system_config)
      synth_lambda_log_group_prefix = "/aws/lambda/cwsyn-"
      cmdline = NHSx::AWS::Commandlines.list_log_groups(synth_lambda_log_group_prefix, region)
      cmd = run_command("Retrieve list of all synth lambda log groups", cmdline, system_config)
      JSON.parse(cmd.output)["logGroups"].map { |el| el["logGroupName"] }
    end

    def get_all_synth_lambda_layers(region, system_config)
      cmdline = NHSx::AWS::Commandlines.list_lambda_layers(region)
      cmd = run_command("Retrieve list of all lambda layers", cmdline, system_config)
      JSON.parse(cmd.output)["Layers"].map {
              |el| el["LayerName"] if el["LayerName"].match(/^(cwsyn-[^:]*)$/) }.compact
    end

    def delete_synth_lambda_functions(active_synth_lambdas, region, system_config)
      all_synth_lambda_functions = get_all_synth_lambda_functions(region, system_config)
      unused_synth_lambda_functions = all_synth_lambda_functions - active_synth_lambdas
      delete_lambda_functions(unused_synth_lambda_functions, region, system_config)
    end

    def delete_synth_lambda_log_groups(active_synth_log_groups, region, system_config)
      all_synth_lambda_log_groups = get_all_synth_log_groups(region, system_config)
      unused_synth_lambda_log_groups = all_synth_lambda_log_groups - active_synth_log_groups
      delete_log_groups(unused_synth_lambda_log_groups, region, system_config)
    end

    def delete_synth_lambda_layers(active_synth_layers, region, system_config)
      all_synth_lambda_layers = get_all_synth_lambda_layers(region, system_config)
      unused_synth_lambda_layers = all_synth_lambda_layers - active_synth_layers
      delete_lambda_layers(unused_synth_lambda_layers, region, system_config)
    end

    def delete_orphan_synth_resources(region, system_config)
      active_synth_engine_id_list = list_active_synth_engine_ids(region, system_config)
      delete_synth_lambda_functions(active_synth_engine_id_list, region, system_config)

      active_synth_lambda_log_group = active_synth_engine_id_list.map{ |el| "/aws/lambda/#{el}"}
      delete_synth_lambda_log_groups(active_synth_lambda_log_group, region, system_config)

      delete_synth_lambda_layers(active_synth_engine_id_list, region, system_config)
    end
  end
end
