require 'digest'
require 'json'
require 'stringio'
require 'tempfile'
require_relative '../../gaudi/helpers/utilities'
require_relative '../../zuehlke/helpers/execution'
require_relative '../../zuehlke/helpers/templates'
require_relative 'target'
require_relative 'terraform'
require_relative 'tf_state_tracker'
require_relative 'versions'

# Please note: this helper module is replicated in the app-system repository (but with slight modifications).
# Changes here should be propagated to there.

module NHSx
  # Methods for Terraform state mutation
  module RefactorTerraform
    include Gaudi::Utilities
    include NHSx::TargetEnvironment
    include NHSx::Terraform
    include NHSx::Versions
    include Zuehlke::Execution
    include Zuehlke::Templates

    TF_REFACTOR_SUMMARY_REMOVED="removed".freeze
    TF_REFACTOR_SUMMARY_SKIPPED_REMOVES="skipped_removes".freeze
    TF_REFACTOR_SUMMARY_IMPORTED="imported".freeze
    TF_REFACTOR_SUMMARY_SKIPPED_IMPORTS="skipped_imports".freeze
    TF_REFACTOR_SUMMARY_MOVED="moved".freeze
    TF_REFACTOR_SUMMARY_SKIPPED_MOVES="skipped_moves".freeze

    def tf_remove_resources(system_config, removals_spec, dry_run=false)
      # Make sure as precondition to call init_terraform(system_config, domain, bubble, account)
      to_remove = []
      to_skip = []

      resource_tracker = tf_state_mirror system_config
      for path in removals_spec.keys.map do
        if resource_tracker.resource_exists path
          to_remove.append(path)
          resource_tracker.remove path
          if !dry_run
            cmdline = "terraform state rm '#{path}' -no-color"
            run_command("removals", cmdline, system_config)
          end
        else
          to_skip.append(path)
        end
      end

      return {
        TF_REFACTOR_SUMMARY_REMOVED => to_remove,
        TF_REFACTOR_SUMMARY_SKIPPED_REMOVES => to_skip,
      }
    end

    def build_tf_import_to_import_message(path, id)
      "#{path} (#{id})"
    end

    def build_tf_import_to_skip_message(path, id)
      "#{path} (#{id})"
    end

    def tf_import_resources(system_config, imports_spec, dry_run=false)
      # Make sure as precondition to call init_terraform(system_config, domain, bubble, account)
      to_import = []
      to_skip = []

      resource_tracker = tf_state_mirror system_config
      imports_spec.each do |path, id|
        if resource_tracker.resource_exists path
          to_skip.append(build_tf_import_to_skip_message(path, id))
        else
          to_import.append(build_tf_import_to_import_message(path, id))
          resource_tracker.import path
          if !dry_run
            cmdline = "terraform import '#{path}' '#{id}' -no-color"
            run_command("imports", cmdline, system_config)
          end
        end
      end

      return {
        TF_REFACTOR_SUMMARY_IMPORTED => to_import,
        TF_REFACTOR_SUMMARY_SKIPPED_IMPORTS => to_skip
      }
    end

    def build_tf_move_to_move_message(source, dest)
      "#{source} -> #{dest}"
    end

    def build_tf_move_already_moved_message(source, dest)
      "Already moved: #{source} -> #{dest}"
    end

    def build_tf_move_source_and_dest_not_found_message(source, dest)
      "Unable to move. Source and dest paths not found: #{source} -> #{dest}"
    end

    def build_tf_move_source_and_dest_found_message(source, dest)
      "Unable to move. Source and dest paths both found: #{source} -> #{dest}"
    end

    def tf_move(system_config, move_spec, dry_run=false)
      to_move = []
      skipped = []

      resource_tracker = tf_state_mirror system_config
      move_spec.each do |source, dest|
        source_exists = resource_tracker.resource_exists source
        dest_exists = resource_tracker.resource_exists dest
        unless source_exists
          if dest_exists
            puts("Resource '#{source}' will not be moved to '#{dest}'.  It's already been moved.")
            skipped.append(build_tf_move_already_moved_message(source, dest))
          else
            puts("Resource '#{source}' will not be moved to '#{dest}'.  Both resource paths not found.")
            skipped.append(build_tf_move_source_and_dest_not_found_message(source, dest))
          end
        else
          if dest_exists
            # Check for corner case where destination path consists only of data resources, which can be safely discarded
            if contains_only_data_resources(system_config, logdir)
              resource_tracker.remove dest
              if !dry_run
                cmdline = "terraform state rm '#{dest}' -no-color"
                run_tee("replacements", cmdline, system_config)
              end
              dest_exists = false
            end
          end
          unless dest_exists
            puts("Resource '#{source}' will be moved to '#{dest}'.")
            to_move.append(build_tf_move_to_move_message(source, dest))

            resource_tracker.move source, dest
            if !dry_run
              cmdline = "terraform state mv '#{source}' '#{dest}' -no-color"
              run_tee("moves", cmdline, system_config)
            end
          else
            puts("Resource '#{source}' will not be moved to '#{dest}'.  Both resource paths found.")
            skipped.append(build_tf_move_source_and_dest_found_message(source, dest))
          end
        end
      end
      return {
        TF_REFACTOR_SUMMARY_MOVED => to_move,
        TF_REFACTOR_SUMMARY_SKIPPED_MOVES => skipped,
      }
    end

    def tf_refactor(system_config, terraform_configuration, tgt_env, account, json_spec, dry_run=false)
      summary = {
        TF_REFACTOR_SUMMARY_REMOVED => [],
        TF_REFACTOR_SUMMARY_SKIPPED_REMOVES => [],
        TF_REFACTOR_SUMMARY_IMPORTED => [],
        TF_REFACTOR_SUMMARY_SKIPPED_IMPORTS => [],
        TF_REFACTOR_SUMMARY_MOVED => [],
        TF_REFACTOR_SUMMARY_SKIPPED_MOVES => [],
      }

      unless dry_run
        # Check that no exception would be raised.
        tf_refactor(system_config, terraform_configuration, tgt_env, account, json_spec, true)
        reset_all_resources_in_tfstate
      end

      Dir.chdir(terraform_configuration) do
        raise GaudiError, "Empty refactoring specification" if json_spec.to_s.strip.empty?
        spec = JSON.parse(json_spec)

        removals_spec = spec['removals']
        if (!removals_spec.nil?)
          result = tf_remove_resources(system_config, removals_spec, dry_run)
          summary[TF_REFACTOR_SUMMARY_REMOVED] = result[TF_REFACTOR_SUMMARY_REMOVED]
          summary[TF_REFACTOR_SUMMARY_SKIPPED_REMOVES] = result[TF_REFACTOR_SUMMARY_SKIPPED_REMOVES]
        end

        imports_spec  = spec['imports']
        if (!imports_spec.nil?)
          result = tf_import_resources(system_config, imports_spec, dry_run)
          summary[TF_REFACTOR_SUMMARY_IMPORTED] = result[TF_REFACTOR_SUMMARY_IMPORTED]
          summary[TF_REFACTOR_SUMMARY_SKIPPED_IMPORTS] = result[TF_REFACTOR_SUMMARY_SKIPPED_IMPORTS]
        end

        moves_spec = spec['moves']
        if (!moves_spec.nil?)
          result = tf_move(system_config, moves_spec, dry_run)
          summary[TF_REFACTOR_SUMMARY_MOVED] = result[TF_REFACTOR_SUMMARY_MOVED]
          summary[TF_REFACTOR_SUMMARY_SKIPPED_MOVES] = result[TF_REFACTOR_SUMMARY_SKIPPED_MOVES]
        end
      end

      return summary
    end

    def build_report_summary_section(summary, report_string_builder, section_name)
      if(!summary[section_name].empty?)
        report_string_builder << "#{section_name.upcase}\n"
        summary[section_name].each {|item| report_string_builder << "  - #{item}\n"}
        report_string_builder << "\n"
      end
    end

    def build_report_summary(summary, dry_run)
      report_string_builder = StringIO.new
      sections = [
        TF_REFACTOR_SUMMARY_IMPORTED,
        TF_REFACTOR_SUMMARY_SKIPPED_IMPORTS,
        TF_REFACTOR_SUMMARY_REMOVED,
        TF_REFACTOR_SUMMARY_SKIPPED_REMOVES,
        TF_REFACTOR_SUMMARY_MOVED,
        TF_REFACTOR_SUMMARY_SKIPPED_MOVES
      ]

      if(dry_run)
        report_string_builder << "Executed in DRY_RUN mode.\n"
      end

      sections.each {|section| build_report_summary_section(summary, report_string_builder, section)}

      report_string_builder.string
    end

    def save_report_summary(system_config, report_summary)
      logfile = File.join(system_config.out, "logs", "#{Time.now.strftime("%Y%m%d_%H%M%S")}_refactor_terraform_summary.log")
      write_file(logfile, report_summary)
    end

  end
end
