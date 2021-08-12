from cryptography.hazmat.primitives.serialization import load_pem_private_key
from json import loads
import boto3
import jwt
import os
import time
import requests
import gzip
import csv
import datetime

ALGORITHM = "ES256"
DATE_FORMAT = "%Y-%m-%d"
FILE_KEY_PREFIX = "apple-sales-"
FORCE_DOWNLOAD_DAYS = 7

def get_secrets():
    secrets_manager = boto3.client('secretsmanager')
    auth_secret = secrets_manager.get_secret_value(SecretId="/apple_sales/auth_key")
    auth_key = auth_secret["SecretString"]
    key_secret = secrets_manager.get_secret_value(SecretId="/apple_sales/key_id")
    key_id = loads(key_secret["SecretString"])["/apple_sales/key_id"]
    issuer_secret = secrets_manager.get_secret_value(SecretId="/apple_sales/issuer_id")
    issuer_id = loads(issuer_secret["SecretString"])["/apple_sales/issuer_id"]
    return {"auth_key": auth_key, "key_id": key_id, "issuer_id": issuer_id}

def generate_jwt_token():
    secrets = get_secrets()

    private_key = load_pem_private_key(secrets["auth_key"].encode("utf-8"), password=None)

    payload = {
        'iss': secrets["issuer_id"],
        'exp': time.time() + 20 * 60,
        'aud': 'appstoreconnect-v1'
    }

    header_fields = {
        'alg': ALGORITHM,
        'kid': secrets["key_id"],
        'typ': 'JWT'
    }

    encoded_jwt = jwt.encode(payload, private_key, ALGORITHM, header_fields)
    return encoded_jwt

def upload(date, bucket, headers):
    endpoint = "https://api.appstoreconnect.apple.com/v1/salesReports?filter[frequency]=DAILY&filter[reportDate]=" + date + "&filter[reportType]=SALES&filter[reportSubType]=SUMMARY&filter[vendorNumber]=89811168"

    response = requests.get(endpoint, headers=headers)
    if response.status_code == 200:
        data = gzip.decompress(response.content).decode("utf-8")

        tab_file_name = "/tmp/apple_sales_tab_separated.txt"
        csv_file_name = "/tmp/apple_sales_comma_separated.txt"
        text_file = open(tab_file_name, "wt")
        text_file.write(data)
        text_file.close()

        in_txt = csv.reader(open(tab_file_name, "r"), delimiter = '\t')
        out_csv = csv.writer(open(csv_file_name, 'w'))
        out_csv.writerows(in_txt)

        with open(csv_file_name) as f:
            string = f.read()
            encoded_string = string.encode("utf-8")

        key = FILE_KEY_PREFIX + date

        bucket.put_object(Key=key, Body=encoded_string)
        print("Uploaded object " + key)
    else:
        print("Request failed for " + date + " with " + str(response.status_code))

def request():
    start_date = datetime.date(2020, 9, 18)
    today = datetime.date.today()
    last_week = (today - datetime.timedelta(days=FORCE_DOWNLOAD_DAYS)).strftime(DATE_FORMAT);
    report_dates = [(start_date + datetime.timedelta(days=x)).strftime(DATE_FORMAT) for x in range(0, (today - start_date).days)]

    s3 = boto3.resource("s3")
    env_name = os.environ["env"]
    bucket_name = env_name + "-analytics-apple-sales-report"
    bucket = s3.Bucket(bucket_name)
    for bucket_object in bucket.objects.all():
        bucket_date = bucket_object.key.lstrip(FILE_KEY_PREFIX)
        if bucket_date < last_week:
            report_dates.remove(bucket_date)

    headers = {"Authorization": "Bearer " + generate_jwt_token()}
    for date in report_dates:
        upload(date, bucket, headers)

def handler(event, context):
    request()
