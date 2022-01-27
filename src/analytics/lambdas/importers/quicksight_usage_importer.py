import csv
import os
import boto3
import datetime
import json

DATE_FORMAT = "%Y-%m-%d"
FILE_KEY_PREFIX = "quicksight-usage-"
BUCKET_SUFFIX = "-analytics-quicksight-usage"
DOWNLOAD_DAYS = 31
EVENT_TIME = "event_time"
EVENT_NAME = "event_name"
USERNAME = "username"
USER_ARN = "user_arn"
ASSUMED_ROLE = "assumed_role"
REGION = "aws_region"
OBJECT_ID = "object_id"
OBJECT_NAME = "object_name"
COLUMNS = [EVENT_TIME, EVENT_NAME, USERNAME, USER_ARN, ASSUMED_ROLE, REGION, OBJECT_ID, OBJECT_NAME]


def get_events(start_date):
    end_date = start_date + datetime.timedelta(days=1)
    cloudtrail = boto3.client("cloudtrail")
    response = cloudtrail.lookup_events(
        LookupAttributes=[
            {
                'AttributeKey': 'EventSource',
                'AttributeValue': 'quicksight.amazonaws.com'
            }
        ],
        StartTime=start_date,
        EndTime=end_date
    )
    events = response["Events"]
    while response.get("NextToken", None) is not None:
        response = cloudtrail.lookup_events(
            LookupAttributes=[
                {
                    'AttributeKey': 'EventSource',
                    'AttributeValue': 'quicksight.amazonaws.com'
                }
            ],
            StartTime=start_date,
            EndTime=end_date,
            NextToken=response["NextToken"]
        )
        events.extend(response["Events"])

    return events


def create_file(date, events, bucket):
    csv_file_name = FILE_KEY_PREFIX + date.strftime(DATE_FORMAT) + ".csv"
    file_path = "/tmp/" + csv_file_name
    try:
        write_events_to_temp_file(events, file_path)
        write_file_to_s3(bucket, csv_file_name, file_path)
    except IOError as error:
        print(error)


def write_events_to_temp_file(events, file_path):
    with open(file_path, 'w') as csvfile:
        writer = csv.DictWriter(csvfile, fieldnames=COLUMNS)
        writer.writeheader()
        for event in events:
            data = event_as_table_record(event)
            writer.writerow(data)


def write_file_to_s3(bucket, csv_file_name, file_path):
    with open(file_path) as f:
        string = f.read()
        encoded_string = string.encode("utf-8")
    bucket.put_object(Key=csv_file_name, Body=encoded_string)


def event_as_table_record(event):
    cloud_trail_event = json.loads(event["CloudTrailEvent"])
    user_identity = cloud_trail_event["userIdentity"]
    session_context = user_identity["sessionContext"]
    session_issuer = session_context["sessionIssuer"]
    response_details = cloud_trail_event.get('serviceEventDetails', {}).get("eventResponseDetails", {})
    dashboard_details = response_details.get("dashboardDetails", None)
    object_name = ""
    object_id = ""

    if dashboard_details:
        object_name = dashboard_details.get("dashboardName", "")
        object_id = dashboard_details.get("dashboardId", "")

    analysis_details = response_details.get("analysisDetails", None)
    if analysis_details:
        object_name = analysis_details.get("analysisName", "")
        object_id = analysis_details.get("analysisId" "")

    data = {
        EVENT_TIME: event.get("EventTime", ""),
        EVENT_NAME: event.get("EventName", ""),
        USERNAME: event.get("Username", ""),
        USER_ARN: user_identity.get("arn", ""),
        ASSUMED_ROLE: session_issuer.get("userName", ""),
        REGION: cloud_trail_event.get("awsRegion", ""),
        OBJECT_ID: object_id,
        OBJECT_NAME: object_name
    }

    return data


def request():
    today = datetime.date.today()
    start_date = (today - datetime.timedelta(days=DOWNLOAD_DAYS))
    report_dates = [start_date + datetime.timedelta(days=x) for x in range(0, (today - start_date).days)]

    s3 = boto3.resource("s3")
    env_name = os.environ["env"]
    bucket_name = env_name + BUCKET_SUFFIX
    bucket = s3.Bucket(bucket_name)
    for bucket_object in bucket.objects.all():
        bucket_date = datetime.datetime.strptime(bucket_object.key.lstrip(FILE_KEY_PREFIX), DATE_FORMAT + ".csv").date()
        if bucket_date in report_dates:
            report_dates.remove(bucket_date)

    for date in report_dates:
        events = get_events(datetime.datetime.combine(date, datetime.datetime.min.time()))
        create_file(date, events, bucket)


def handler(event, context):
    request()
