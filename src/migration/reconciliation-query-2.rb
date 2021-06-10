#!/usr/bin/ruby

# usage:
# 1) TARGET_ENV=te-ci ./src/migration/reconciliation-query-2.rb | pbcopy
# 2) paste SQL into Athena query window

env=ENV['TARGET_ENV'].nil? ? "te-load-test" : ENV['TARGET_ENV']

# below: if the migration did work fine, this query must return 0 rows
puts <<-eos
select source.part as source_part, source.cnt as source_count, target.part as target_part, target.cnt as target_count from (
  select date_parse(submitteddatehour,'%Y/%c/%d/%H') as part, count(*) as cnt
  FROM "#{env}_analytics_db"."#{env}_analytics_mobile" 
  group by date_parse(submitteddatehour,'%Y/%c/%d/%H')
) source 
full outer join
(
  select date_parse(submitteddatehour,'%Y-%c-%d-%H') as part, count(*) as cnt
  FROM "#{env}_analytics_db"."#{env}_analytics_mobile_consolidated" 
  group by date_parse(submitteddatehour,'%Y-%c-%d-%H')
) target
on source.part=target.part
having source.part is null or target.part is null or source.cnt != target.cnt
eos
