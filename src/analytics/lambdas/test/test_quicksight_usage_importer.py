from . import check_code
from importers import quicksight_usage_importer
from datetime import datetime


def test_code_is_valid():
    assert check_code('importers/quicksight_usage_importer.py')


def test_event_as_table_record():
    cloud_trail_event = """{
  "eventVersion": "1.08",
  "userIdentity": {
    "type": "AssumedRole",
    "principalId": "EXAMPLE:aa-ci-aa-ci-quicksight_data_sets_importer",
    "arn": "arn:aws:sts::00000000:assumed-role/example/quicksight_data_sets_importer",
    "accountId": "000000",
    "accessKeyId": "EXAMPLE",
    "sessionContext": {
      "sessionIssuer": {
        "type": "Role",
        "principalId": "EXAMPLE",
        "arn": "arn:aws:iam::00000000:role/example",
        "accountId": "000000",
        "userName": "example"
      },
      "webIdFederationData": {},
      "attributes": {
        "creationDate": "2022-01-13T02:00:09Z",
        "mfaAuthenticated": "false"
      }
    }
  },
  "eventTime": "2022-01-13T02:04:13Z",
  "eventSource": "quicksight.amazonaws.com",
  "eventName": "ListIngestions",
  "awsRegion": "eu-west-2",
  "sourceIPAddress": "127.0.0.1",
  "userAgent": "Boto3",
  "requestParameters": {
    "dataSetId": "example",
    "awsAccountId": "000000"
  },
  "responseElements": null,
  "requestID": "example",
  "eventID": "example",
  "readOnly": true,
  "eventType": "AwsApiCall",
  "managementEvent": true,
  "recipientAccountId": "000000",
  "eventCategory": "Management",
  "tlsDetails": {
    "tlsVersion": "TLSv1.2",
    "clientProvidedHostHeader": "quicksight.eu-west-2.amazonaws.com"
  }
}"""
    event = {
                'EventId': 'exampleEventId',
                'EventName': 'exampleEvent',
                'ReadOnly': 'NO',
                'AccessKeyId': 'example',
                'EventTime': datetime(2015, 1, 1),
                'EventSource': 'quicksight.eu-west-2.amazonaws.com',
                'Username': 'testUser',
                'Resources': [
                    {
                        'ResourceType': 'string',
                        'ResourceName': 'string'
                    },
                ],
                'CloudTrailEvent': cloud_trail_event
            }
    table_record = quicksight_usage_importer.event_as_table_record(event)
    expected_table_record = {
        'event_time': datetime(2015, 1, 1, 0, 0),
        'event_name': 'exampleEvent',
        'username': 'testUser',
        'user_arn': 'arn:aws:sts::00000000:assumed-role/example/quicksight_data_sets_importer',
        'assumed_role': 'example',
        'aws_region': 'eu-west-2',
        'object_id': '',
        'object_name': ''
    }

    assert table_record == expected_table_record
