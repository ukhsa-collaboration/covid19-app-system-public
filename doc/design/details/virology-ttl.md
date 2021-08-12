# Virology Tokens TTL

The virology tokens stored in DynamoDB tables have a time-to-live attribute. These tokens are then deleted after they expire.


## Configuration 
* The ttls are configured in the lambda.


### Initial TTL after token generation 

  Table Name|Expiry|
  ---|---|
  *-virology-ordertokens|4 weeks|
  *-virology-testresults|4 weeks|
  *-virology-submissiontokens (only positive results)|4 weeks|

### Events which will adjust TTLs

* Positive test result uploaded via Test Lab API 

  Table Name|Expiry|
  ---|---|
  *-virology-submissiontokens|4 weeks|

* CTA exchange (Can be exchanged max 2 times)

  Table Name|Expiry|
  ---|---|
  *-virology-ordertokens|4 hours|
  *-virology-testresults|4 hours|
  *-virology-submissiontokens (only positive results)|4 days|
