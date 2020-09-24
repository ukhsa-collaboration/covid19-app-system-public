require "json"

module NHSx
  # Helpers to codify the restore process for the virology database
  module RestoreVirology
    include Zuehlke::Execution
    include NHSx::TargetEnvironment
    # Interval to wait between repeated AWS requests
    WAIT_TIME = 10
    # Restore the virology data from a point in time backup for the given environment in _account_
    #
    # point_in_time is an epoch timestamp in seconds
    #
    # The restore process is as follows:
    #
    # For each of the virology tables the data from the chosen point in time is restored in a table matching the table name suffixed with -restore
    #
    # The original table is deleted
    #
    # A backup is created from the restored table and that backup is used to restore the data with the original table name
    #
    # The table attributes (point-in-time backups, TTL, etc.) are subsequently restored
    def restore_virology(tgt_env, account, point_in_time, system_config)
      target_environment_config = target_environment_configuration(tgt_env, account, system_config)
      original_tables = [target_environment_config["virology_table_submission_tokens"],
                         target_environment_config["virology_table_results"],
                         target_environment_config["virology_table_test_orders"]]
      restore_original_tables(original_tables, point_in_time, system_config)
      restore_table_attributes(original_tables, system_config)
    end

    def restore_tables_from_point_in_time(original_tables, point_in_time, system_config)
      restored_tables = original_tables.map do |table_name|
        restore_table(table_name, point_in_time, system_config)
      end
      wait_for_table_restoration(restored_tables, system_config)
      return restored_tables
    end

    def delete_tables(table_list, system_config)
      table_list.each do |tn|
        delete_dynamodb_table(tn, system_config)
      end
      # Wait till the tables are deleted
      deleted_table_stati = [true]
      while deleted_table_stati.include?(true)
        deleted_table_stati = table_list.map do |table_name|
          table_exists?(table_name, system_config)
        end
        sleep WAIT_TIME if deleted_table_stati.include?(true)
      end
    end

    def restore_original_tables(original_tables, point_in_time, system_config)
      restore_tables_from_point_in_time(original_tables, point_in_time, system_config)
      delete_tables(original_tables, system_config)

      original_tables.each do |tn|
        restored_table_name = "#{tn}-restored"
        backup_arn = create_restored_table_backup(restored_table_name, system_config)
        restore_table_from_backup(tn, backup_arn, system_config)
      end

      wait_for_table_restoration(original_tables, system_config)
    end

    def wait_for_table_restoration(list_of_tables, system_config)
      table_stati = [false]
      while table_stati.include?(false)
        table_stati = list_of_tables.map do |table_name|
          table_restored?(table_name, system_config)
        end
        sleep WAIT_TIME if table_stati.include?(false)
      end
    end

    def restore_table_attributes(original_tables, system_config)
      original_tables.each do |tn|
        enable_point_in_time_recovery(tn, system_config)
        enable_time_to_live(tn, system_config)
      end
    end

    def clean_restored_tables(target_environment, system_config)
      cmdline = "aws dynamodb list-tables"
      cmd = run_command("List DynamoDB tables", cmdline, system_config)
      all_tables = JSON.parse(cmd.output)
      # only get the tables for the current environment that have been used for restore
      restore_tables = all_tables["TableNames"].select { |tn| /#{target_environment}.+-restored/ =~ tn }
      restore_tables.each { |tn| delete_dynamodb_table(tn, system_config) }
      return restore_tables
    end

    # Trigger a dynamodb table restoration from a point in time backup
    #
    # The point in time is an epoch timestamp (seconds since epoch)
    #
    # Returns the name of the table created with the restored data
    def restore_table(table_name, restoration_time, system_config)
      restored_table = "#{table_name}-restored"

      cmdline = "aws dynamodb restore-table-to-point-in-time --source-table-name #{table_name} --target-table-name #{restored_table} --restore-date-time #{restoration_time}"
      puts "* Restoring #{table_name} to #{restored_table}"
      run_command("Restore #{table_name}", cmdline, system_config)
      return restored_table
    end

    def restore_table_from_backup(table_name, backup_arn, system_config)
      cmdline = "aws dynamodb restore-table-from-backup --target-table-name #{table_name} --backup-arn #{backup_arn}"
      run_command("Restore #{table_name}", cmdline, system_config)
    end

    def enable_point_in_time_recovery(table_name, system_config)
      cmdline = "aws dynamodb update-continuous-backups --table-name #{table_name} --point-in-time-recovery-specification PointInTimeRecoveryEnabled=true"
      run_command("Enable #{table_name} point in time", cmdline, system_config)
    end

    def enable_time_to_live(table_name, system_config)
      cmdline = "aws dynamodb update-time-to-live --table-name #{table_name} --time-to-live-specification Enabled=true,AttributeName=expireAt"
      run_command("Enable #{table_name} time to live", cmdline, system_config)
    end

    def table_restored?(table_name, system_config)
      cmdline = "aws dynamodb describe-table --table-name #{table_name}"
      cmd = run_command("Describe table #{table_name}", cmdline, system_config)
      table_status = JSON.parse(cmd.output)
      return table_status["Table"]["TableStatus"] == "ACTIVE"
    end

    def table_exists?(table_name, system_config)
      cmdline = "aws dynamodb describe-table --table-name #{table_name}"
      run_command("Describe table #{table_name}", cmdline, system_config)
      return true
    rescue GaudiError
      return false
    end

    def delete_dynamodb_table(table_name, system_config)
      cmdline = "aws dynamodb delete-table --table-name #{table_name}"
      run_command("Delete table #{table_name}", cmdline, system_config)
    end

    # Create a named DynamoDB backup from a table we restored via point-in-time restore
    # Returns the ARN of the backup
    def create_restored_table_backup(restored_table_name, system_config)
      backup_name = "#{restored_table_name}-backup"
      cmdline = "aws dynamodb create-backup --table-name #{restored_table_name} --backup-name #{backup_name}"
      cmd = run_command("Backup #{restored_table_name}", cmdline, system_config)
      JSON.parse(cmd.output)["BackupDetails"]["BackupArn"]
    end
  end
end
