namespace :parquet do

  def diff_glue_tables(glue_table_1, glue_table_2)
    columns_1 = glue_table_1["StorageDescriptor"]["Columns"]
    columns_2 = glue_table_2["StorageDescriptor"]["Columns"]

    diff_not_in_1 = columns_2 - columns_1
    diff_not_in_2 = columns_1 - columns_2

    raise GaudiError, "glue columns mismatch, not in #{glue_table_1["Name"]}: #{diff_not_in_1}" unless diff_not_in_1.empty?
    raise GaudiError, "glue columns mismatch, not in #{glue_table_2["Name"]}: #{diff_not_in_2}" unless diff_not_in_2.empty?
  end

  def run_athena_query(query, query_name, workgroup)
    query_execution_id = start_athena_query(query, workgroup, $configuration)

    running = true
    while running
      results = get_athena_query_execution(query_execution_id, $configuration)
      p results["Status"]
      running = %w[QUEUED RUNNING].include?(results["Status"]["State"])
      if running
        puts "."
        sleep 5
      end
    end

    raise GaudiError, "query did not complete successfully" if results["Status"]["State"] != "SUCCEEDED"

    query_results = get_athena_query_results(query_execution_id, $configuration)
    csv = query_results.map { |row| row["Data"].map { |col| col["VarCharValue"] }.join(",") }.join("\n")
    query_results_file = File.join($configuration.out, "parquet", "#{Time.now.to_i}-#{query_name}-#{query_execution_id}.csv")
    write_file(query_results_file, csv)

    query_results
  end

  NHSx::TargetEnvironment::CTA_TARGET_ENVIRONMENTS.each do |account, tgt_envs|
    tgt_envs.each do |tgt_env|
      desc "Reconcile parquet files query 1/2"
      task :"rec:q1:#{tgt_env}" => [:"login:#{account}"] do
        include NHSx::AWS
        prefix = target_environment_name(tgt_env, account, $configuration)

        cta_glue_table = get_glue_table("#{prefix}-analytics", "#{prefix}_analytics", $configuration)
        cta_consolidated_glue_table = get_glue_table("#{prefix}-analytics", "#{prefix}_analytics_consolidated", $configuration)
        analytics_glue_table = get_glue_table("#{prefix}_analytics_db", "#{prefix}_analytics_mobile", $configuration)
        analytics_consolidated_glue_table = get_glue_table("#{prefix}_analytics_db", "#{prefix}_analytics_mobile_consolidated", $configuration)

        diff_glue_tables(cta_glue_table, cta_consolidated_glue_table)
        diff_glue_tables(cta_glue_table, analytics_glue_table)
        diff_glue_tables(cta_glue_table, analytics_consolidated_glue_table)

        projections = analytics_glue_table["StorageDescriptor"]["Columns"].filter_map { |column|
          column_name = column["Name"]
          column["Type"] == "int" ? "sum(coalesce(#{column_name},0)) as #{column_name}" : nil
        }.join(",")

        consolidated_end_partition = ENV['CONSOLIDATED_END_PARTITION'].nil? ? Time.now.utc.to_s[0..9] + "-00" : ENV['CONSOLIDATED_END_PARTITION']
        source_end_partition = consolidated_end_partition.gsub('-', '/')

        query = <<-eos
select count(*) as cnt,min(submitteddatehour) as mindate,max(submitteddatehour) as maxdate,#{projections}
FROM "#{prefix}_analytics_db"."#{prefix}_analytics_mobile"
where submitteddatehour < '#{source_end_partition}'
union all
select count(*) as cnt,min(submitteddatehour) as mindate,max(submitteddatehour) as maxdate,#{projections}
FROM "#{prefix}_analytics_db"."#{prefix}_analytics_mobile_consolidated"
where submitteddatehour < '#{consolidated_end_partition}'
        eos

        query_results = run_athena_query(query, "q1", "#{prefix}_analytics_quicksight")

        raise GaudiError, "expected 3 rows but got #{query_results.length}" if query_results.length != 3

        header_row = query_results[0]["Data"]
        data_row_1 = query_results[1]["Data"]
        data_row_2 = query_results[2]["Data"]

        raise GaudiError, "rows have different lengths" if header_row.length != data_row_1.length or header_row.length != data_row_2.length

        differences = header_row.filter_map.with_index { |header, i|
          header_name = header["VarCharValue"]
          string_value_1 = data_row_1[i]["VarCharValue"]
          string_value_2 = data_row_2[i]["VarCharValue"]

          if header_name == "mindate" or header_name == "maxdate"
            if string_value_1.include? "/"
              date_value_1 = DateTime.strptime(string_value_1, "%Y/%m/%d/%H")
              date_value_2 = DateTime.strptime(string_value_2, "%Y-%m-%d-%H")
            else
              date_value_1 = DateTime.strptime(string_value_1, "%Y-%m-%d-%H")
              date_value_2 = DateTime.strptime(string_value_2, "%Y/%m/%d/%H")
            end
            "#{header_name}, #{string_value_1} != #{string_value_2}" unless date_value_1 == date_value_2
          elsif string_value_1 != string_value_2
            "#{header_name}, #{string_value_1} != #{string_value_2}"
          end
        }.each { |diff| puts diff }

        raise GaudiError, "there are differences in source and destination" unless differences.empty?

      end

      desc "Reconcile parquet files query 2/2"
      task :"rec:q2:#{tgt_env}" => [:"login:#{account}"] do
        prefix = target_environment_name(tgt_env, account, $configuration)

        # below: if the migration did work fine, this query must return 0 rows
        query = <<-eos
select source.part as source_part, source.cnt as source_count, target.part as target_part, target.cnt as target_count from (
  select date_parse(submitteddatehour,'%Y/%c/%d/%H') as part, count(*) as cnt
  FROM "#{prefix}_analytics_db"."#{prefix}_analytics_mobile" 
  group by date_parse(submitteddatehour,'%Y/%c/%d/%H')
) source 
full outer join
(
  select date_parse(submitteddatehour,'%Y-%c-%d-%H') as part, count(*) as cnt
  FROM "#{prefix}_analytics_db"."#{prefix}_analytics_mobile_consolidated" 
  group by date_parse(submitteddatehour,'%Y-%c-%d-%H')
) target
on source.part=target.part
having source.part is null or target.part is null or source.cnt != target.cnt
        eos

        query_results = run_athena_query(query, "q2", "#{prefix}_analytics_quicksight")
        raise GaudiError, "expected 1 row but got #{query_results.length}" unless query_results.length == 1

        header_row = query_results[0]["Data"].map { |header| header["VarCharValue"] }
        raise GaudiError, "header row does not match" unless header_row == %w[source_part source_count target_part target_count]
      end

      desc "Run all reconciliation queries"
      task :"rec:all:#{tgt_env}" => [:"login:#{account}"] do
        Rake::Task["parquet:rec:q1:#{tgt_env}"].invoke
        Rake::Task["parquet:rec:q2:#{tgt_env}"].invoke
      end
    end
  end
end
