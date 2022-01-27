import requests
from json import dumps, loads
import csv
import os
import boto3
from datetime import datetime
from requests.auth import HTTPBasicAuth
import collections
import ast

platform_dict = {
    "ios": "https://api.appbot.co/api/v2/apps/2411517/ratings/historical?country=&end=" + datetime.today().strftime(
        '%Y-%m-%d') + "&start=2020-09-19",
    "android": "https://api.appbot.co/api/v2/apps/2411517/ratings/historical?end=" + datetime.today().strftime(
        '%Y-%m-%d') + "&start=2020-09-19"}


def get_secrets():
    secretsmanager = boto3.client('secretsmanager')
    api_key = secretsmanager.get_secret_value(SecretId="/appbot/api_key")
    api_password = secretsmanager.get_secret_value(SecretId="/appbot/api_password")

    return {"api_key": api_key, "api_password": api_password}


def flatten(d):
    items = []
    for k, v in d.items():
        if isinstance(v, collections.MutableMapping):
            items.extend(flatten(v).items())
        else:
            items.append((k, v))
    return dict(items)


def request_data(platform):
    secrets = get_secrets()
    r = requests.get(platform_dict[platform],
                     auth=HTTPBasicAuth(loads(secrets["api_key"]["SecretString"])["/appbot/api_key"],
                                        loads(secrets["api_password"]["SecretString"])["/appbot/api_password"]))

    body = loads(r.content.decode())["all_time"]
    csv_columns = [key for key in flatten(body[0])]
    csv_columns.remove('version')

    csv_file = f"analytics-{platform}-lookup.csv"
    lambda_path = "/tmp/" + csv_file
    try:
        with open(lambda_path, 'w') as csvfile:
            writer = csv.DictWriter(csvfile, fieldnames=csv_columns)
            writer.writeheader()

            for data in body:
                del data['version']
                writer.writerow(flatten(data))

        with open(lambda_path) as f:
            string = f.read()
            encoded_string = string.encode("utf-8")

        env_name = os.environ["env"]
        bucket_name = env_name + "-analytics-" + platform + "-historical-ratings"
        s3_path = csv_file
        s3 = boto3.resource("s3")
        s3.Bucket(bucket_name).put_object(Key=s3_path, Body=encoded_string)

    except IOError:
        print("I/O error")


def handler(event, context):
    request_data("android")
    request_data("ios")


if __name__ == "__main__":
    handler("", "")
