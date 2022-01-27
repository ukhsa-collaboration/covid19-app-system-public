import csv
import os
import boto3

COLUMNS = ["Active", "Arn", "CustomPermissionsName", "Email", "IdentityType", "PrincipalId", "Role", "UserName"]


def request_data():
    sts = boto3.client('sts')
    accountId = sts.get_caller_identity().get('Account')

    quicksight = boto3.client("quicksight")
    response = quicksight.list_users(AwsAccountId=accountId, Namespace="default")
    user_list = response["UserList"]
    while response.get("NextToken", None) is not None:
        response = quicksight.list_users(AwsAccountId=accountId, Namespace="default", NextToken=response["NextToken"])
        user_list = user_list + response["UserList"]

    csv_file = "analytics-quicksight-users.csv"
    lambda_path = "/tmp/" + csv_file

    try:
        with open(lambda_path, 'w') as csvfile:
            writer = csv.DictWriter(csvfile, fieldnames=COLUMNS)
            writer.writeheader()
            for user in user_list:
                data = {}
                for column in COLUMNS:
                    data[column] = user.get(column, "")
                writer.writerow(data)

        with open(lambda_path) as f:
            string = f.read()
            encoded_string = string.encode("utf-8")

        env_name = os.environ["env"]
        bucket_name = env_name + "-analytics-quicksight-users"
        s3_path = csv_file
        s3 = boto3.resource("s3")
        s3.Bucket(bucket_name).put_object(Key=s3_path, Body=encoded_string)

    except IOError:
        print("I/O error")


def handler(event, context):
    request_data()
