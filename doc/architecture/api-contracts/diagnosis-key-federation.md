# Diagnosis Key Federation

Federation upload and download Lambda 

- Scheduled to run every four hours starting from 00:00 UTC

## Download of diagnosis keys (or Import)

- The lambda downloads batches of diagnosis keys using NearForm Federation API , the keys are then validated and uploaded to the diagnosis keys submission bucket.
- The keys downloaded using nearform API are stored in the following format

  ```<federated_key_prefix>/<region>/<downloadDate>/<filename>.json```
- The last downloaded batchtag is stored in a table ```<workspace-name>--federation-key-proc-history```

### Validation rules used to accept the keys from Federation server
- Keys should not be older than 14 days or have a future date.
- Keys should be Base64 encoded and 32 bytes in length.
- Transmission risk level should be >=0 and <=7.
- Keys should be from a valid region.
- Rolling period of the keys should be equal to 44.


## Upload of diagnosis keys (or Export) 

- Lambda uploads the diagnosis keys from the submission bucket to the Nearform server using Nearform Federation API.
- The Submission date of the uploaded keys is stored in a table ```<workspace-name>--federation-key-proc-history```
- We can configure the federation upload and download lambda for the region to run and the list of valid regions we would like to exchange the keys.

### For ex:

   If we want to run the lambda for the region "GB-EAW" and want to exchange the keys with France and GB, we would set the REGION to "GB-EAW" and VALID_REGIONS to      "FR,GB".

- We also have a dashboard to view the number of keys downloaded and uploaded per region.
