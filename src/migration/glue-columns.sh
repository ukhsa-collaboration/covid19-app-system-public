#!/bin/bash

echo 'cat src/analytics/modules/mobile_analytics/main.tf | ./src/migration/glue-columns.rb'
cat src/analytics/modules/mobile_analytics/main.tf | ./src/migration/glue-columns.rb 

echo 'cat src/aws/submissions.tf | ./src/migration/glue-columns.rb'
cat src/aws/submissions.tf | ./src/migration/glue-columns.rb 

echo Diffs

echo diff mobile_analytics-cols.txt mobile_analytics_consolidated-cols.txt 
diff mobile_analytics-cols.txt mobile_analytics_consolidated-cols.txt 

echo diff mobile_analytics-cols.txt this-cols.txt 
diff mobile_analytics-cols.txt this-cols.txt 

echo diff mobile_analytics-cols.txt this_consolidated-cols.txt 
diff mobile_analytics-cols.txt this_consolidated-cols.txt 
