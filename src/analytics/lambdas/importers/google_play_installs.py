import os
import boto3
from datetime import date
from google.cloud import storage

data_earliest_year = 2020
data_earliest_month_in_earliest_year = 8


def get_months_covering_data_as_of_today():
    today = date.today()
    return get_months_covering_data(today.year, today.month)


def get_months_covering_data(year, month):
    """
    :return: A list of strings in the format YYYYMM, each item representing a sequential month from
    and including the given year and month, back to and including the year and month of the
    'data_earliest_year' and 'data_earliest_month_in_earliest_year' fields. Note the MM part contains
    a zero-padded month number where needed.
    """

    result = []

    while year > data_earliest_year:
        result.append(str(year) + f'{month:02d}')
        month -= 1
        if month == 0:
            month = 12
            year -= 1

    if year == data_earliest_year:
        while month >= data_earliest_month_in_earliest_year:
            result.append(str(year) + f'{month:02d}')
            month -= 1

    return result


def handler(event, context):
    # Get secret key
    secret_id = os.environ['secret_id']

    print("Access AWS Secrets Manager")
    secretsmanager = boto3.client('secretsmanager')
    response = secretsmanager.get_secret_value(SecretId=secret_id)

    key_file_name = "/tmp/api.json"

    print("Write API key to temporary file")
    key_file = open(key_file_name, "w")
    key_file.write(response['SecretString'])
    key_file.close()

    print("Connect to Google Play GCP Storage service via API key")
    storage_client = storage.Client.from_service_account_json(key_file_name)

    cloud_storage_bucket = 'some-bucket'

    print("Connect to GCP bucket")
    source_bucket = storage.Bucket(storage_client, cloud_storage_bucket)

    print("Connect to AWS S3 service")
    s3 = boto3.resource('s3')

    target_bucket_name = os.environ['target_bucket_name']

    print("Connect to AWS bucket " + target_bucket_name)
    target_bucket = s3.Bucket(target_bucket_name)

    prefix = 'stats/installs/installs_uk.nhs.covid19.production_'
    suffix = '_overview.csv'

    # Determine dates to get Google reports
    months = get_months_covering_data_as_of_today()
    length = len(months)
    i = 0
    while i < length:

        object_name = prefix + months[i] + suffix

        print(str(i + 1) + "/" + str(length) + ": " + object_name)

        try:

            # Download Google reports
            print("Download from GCP bucket")
            blob = source_bucket.get_blob(object_name)
            blob_text = blob.download_as_text(encoding='utf16')
            blob_bytes_utf8 = blob_text.encode('utf-8')

            # Upload reports to our AWS S3 bucket
            print("Upload to AWS bucket")
            target_bucket.put_object(
                Key=object_name,
                Body=blob_bytes_utf8
            )

        except Exception as e:
            print(e)

        i += 1
