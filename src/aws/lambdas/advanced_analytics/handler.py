import logging
import os
import os.path
from collections import namedtuple

import aws_xray_sdk
import boto3
from advanced_analytics import AdvancedAnalytics
from aws_xray_sdk.core import patch_all as xray_patch_all
from aws_xray_sdk.core import xray_recorder

logging.basicConfig(format='[%(levelname)s] %(asctime)s %(message)s', datefmt='%y-%m-%dT%I:%M:%S%Z')
log = logging.getLogger(__name__)
logging.getLogger().setLevel(logging.INFO)
logging.getLogger("aws_xray_sdk.core.recorder").setLevel(logging.ERROR)
logging.getLogger("aws_xray_sdk.core.patcher").setLevel(logging.ERROR)
logging.getLogger("botocore.credentials").setLevel(logging.ERROR)

host = os.environ['AAE_HOSTNAME']
certificate_secret_name = '/aae/advanced_analytics/private-certificate-10593'
key_secret_name = '/aae/advanced_analytics/private-key-10593'
subscription_key_name = '/aae/advanced_analytics/subscription-key-10593'
encryption_password_secret_name = '/aae/advanced_analytics/certificate-encryption-password-10593'

Secrets = namedtuple("Secrets", ["certificate_key", "subscription_key", "certificate_private", "encryption_passphrase"])

aws_xray_sdk.core.context.Context.handle_context_missing = lambda x: None


def grab_secrets(secrets_manager):
    return Secrets(
        certificate_key=secrets_manager.get_secret_value(SecretId=key_secret_name)["SecretString"],
        subscription_key=secrets_manager.get_secret_value(SecretId=subscription_key_name)["SecretString"],
        certificate_private=secrets_manager.get_secret_value(SecretId=certificate_secret_name)["SecretString"],
        encryption_passphrase=secrets_manager.get_secret_value(SecretId=encryption_password_secret_name)["SecretString"]
    )


xray_patch_all()

secrets = grab_secrets(boto3.client("secretsmanager"))

s3 = boto3.resource('s3')
aa = AdvancedAnalytics(host, secrets)


@xray_recorder.capture('Advanced Analytics')
def handler(message, context):
    storage = message["Records"][0]["s3"]
    bucket_name = storage["bucket"]["name"]

    analytics = read_bucket(bucket_name, storage["object"]["key"])
    aa.send(os.path.basename(storage["object"]["key"]), analytics)


def read_bucket(bucket_name, key):
    return s3.Object(bucket_name, key).get()["Body"].read()


if __name__ == "__main__":
    handler({
        "Records": [
            {
                "s3":
                    {
                        "bucket": {
                            "name": "xxxx"
                        },
                        "object": {
                            "key": "yyyy"
                        }
                    }
            }
        ]
    }, None
    )
