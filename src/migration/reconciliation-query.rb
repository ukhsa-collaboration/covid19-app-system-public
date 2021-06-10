#!/usr/bin/ruby

# usage:
# 1) ./src/migration/glue-columns.sh 
# 2) cat this-cols.txt | TARGET_ENV=te-ci CONSOLIDATED_END_PARTITION=2021-05-18-12 ./src/migration/reconciliation-query.rb | pbcopy
# 3) paste SQL into Athena query window

env=ENV['TARGET_ENV'].nil? ? "te-load-test" : ENV['TARGET_ENV']

consoldidated_end_partition=ENV['CONSOLIDATED_END_PARTITION'].nil? ? Time.now.utc.to_s[0..9]+"-00" : ENV['CONSOLIDATED_END_PARTITION'] 
source_end_partition=consoldidated_end_partition.gsub('-','/')

col = nil
typ = nil
query = "select count(*) as cnt,min(submitteddatehour) as mindate,max(submitteddatehour) as maxdate"
while line = gets
    if line =~ /name = "(.+)"/
        col = $1
        typ = nil
    end
    if line =~ /type = "(.+)"/
        typ = $1
    end

    if col != nil and typ != nil
        if typ == 'int'
            if query != ""
                query += ","
            end
            query += "sum(coalesce(#{col},0)) as #{col}"
        end

        col = nil
        typ = nil     
    end
end

# below: if the migration did work fine, this query must return 2 rows and all columns (except mindate and maxdate) must have identical values
puts <<-eos
#{query} 
FROM "#{env}_analytics_db"."#{env}_analytics_mobile" 
where submitteddatehour < '#{source_end_partition}'
union all
#{query} 
FROM "#{env}_analytics_db"."#{env}_analytics_mobile_consolidated"  
where submitteddatehour < '#{consoldidated_end_partition}'
eos
