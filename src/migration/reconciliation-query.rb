#!/usr/bin/ruby
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

puts """#{query} FROM \"te-load-test_analytics_db\".\"te-load-test_analytics_mobile\" where submitteddatehour < '2021/05/06/00'
union all
#{query} FROM \"te-load-test_analytics_db\".\"te-load-test_analytics_mobile_consolidated\"  where submitteddatehour < '2021-05-06-00'
"""
