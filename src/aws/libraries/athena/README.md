# About the Athena queries

- Search Control Panel on Confluence

The queries here do not cover all the metrics from the page, but only the ones that can be derived with information that we recieve from mobiles. 


## Naming structure
Currently, the queries are named after a specific structure, comprising of two parts.
`<metric>_mode`
The metric relates to the metric listed on confluence. 

The mode maps to one of four variants:

| mode              | meaning                                            | query structure                                                                                              |
|-------------------|----------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| total             | the total amount, as a single number               | either the sum or count of a field, without any grouping or ordering                                         |
| beginningOfTime | the daily collected values since our records began | either the sum or count of a field, grouped by date, ordered by date descending                              |
| lastSevenDays         | the daily collected values over the last 7 days    | either the sum or count of a field, grouped by date, ordered by date descending, limiting to the last 7 days |
| last_day          | the value of the last day                         | either the sum or count of a field, grouped by date, ordered by date descending, limiting to the last days   |

### For future queries
This might be useful for when we create future queries, and need to identify what our data means:
```text
Has the user isolated at any point today? isIsolatingBackgroundTick > 0
Has the user isolated all of today?  isIsolatingBackgroundTick == runningNormallyBackgroundTick
Was the app non-functional at any point today? runningNormallyBackgroundTick != totalBackgroundTasks
```