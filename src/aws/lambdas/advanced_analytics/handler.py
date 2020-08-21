import os

import boto3
from aws_xray_sdk.core import patch_all
from aws_xray_sdk.core import xray_recorder

from advanced_analytics import AdvancedAnalytics

global certificate_private, certificate_key, subscription_key, encryption_passphrase

host = os.environ['AAE_ENVIRONMENT']
certificate_secret_name = '/aae/advanced_analytics/private-certificate-10593'
key_secret_name = '/aae/advanced_analytics/private-key-10593'
subscription_key_name = '/aae/advanced_analytics/subscription-key-10593'
encryption_password_secret_name = '/aae/advanced_analytics/certificate-encryption-password-10593'


def grab_secrets():
    secrets_manager = boto3.client("secretsmanager")
    global certificate_private, certificate_key, subscription_key, encryption_passphrase
    certificate_key = secrets_manager.get_secret_value(SecretId=key_secret_name)["SecretString"]
    subscription_key = secrets_manager.get_secret_value(SecretId=subscription_key_name)["SecretString"]
    certificate_private = secrets_manager.get_secret_value(SecretId=certificate_secret_name)["SecretString"]
    encryption_passphrase = secrets_manager.get_secret_value(SecretId=encryption_password_secret_name)["SecretString"]


patch_all()  # patch Boto and HTTPclients that Boto uses to call Amazon SQS and Amazon S3
grab_secrets()
s3 = boto3.resource('s3')
aa = AdvancedAnalytics(certificate_private, certificate_key, subscription_key, encryption_passphrase, host)


@xray_recorder.capture('Advanced Analytics')
def handler(message, context):
    storage = message["Records"][0]["s3"]
    bucket_name = storage["bucket"]["name"]
    document_name = storage["object"]["key"]

    analytics = read_bucket(bucket_name, document_name)
    aa.send(document_name, analytics)


def read_bucket(bucket_name, key):
    return s3.Object(bucket_name, key).get()["Body"].read().decode("utf-8")
