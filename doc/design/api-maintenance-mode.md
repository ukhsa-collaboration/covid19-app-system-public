# Maintenance Mode

All Java lambdas expect the environment variable MAINTENANCE_MODE to be present. 

Setting this variable to true results in the lambda function returning 503_SERVICE_UNAVAILABLE for all requests. 

Expected use for this feature is to facilitate backup and restore procedures and other maintenance of backend services. 

Clients are expected to retry after receiving a 503.

 