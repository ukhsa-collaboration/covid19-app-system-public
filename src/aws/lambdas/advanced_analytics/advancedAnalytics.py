import boto3
import http.client
import json
import logging
import ssl
import os
from botocore.exceptions import ClientError

logging.basicConfig(level=logging.INFO)


def handler(message, context):
    storage = message["Records"][0]["s3"]
    bucket_name = storage["bucket"]["name"]
    document_name = storage["object"]["key"]

    analytics = BucketReader(bucket_name, document_name).read()

    AdvancedAnalytics(analytics).send(document_name)

class BucketReader:
    def __init__(self, bucket_name, key):
        self.s3 = boto3.resource('s3')
        self.bucket_name = bucket_name
        self.key = key

    def read(self):
        s3_object = self.s3.Object(self.bucket_name, self.key)
        return s3_object.get()["Body"].read().decode("utf-8")

class Analytics:

    def __init__(self, analytics):
        self.anlytics = analytics

    def read_metrics(self):
        return json.loads(self.anlytics)

class AdvancedAnalytics(Analytics):

    def __init__(self, analytics):
        super().__init__(analytics)
        self.secrets_manager = boto3.client("secretsmanager")

    def send(self, document_name):
        certificate_id = "certificate_private"
        certificate_key_id = "certificate_key"
        subscription_key_id = "subscription_key"
        encryption_password_id = "encryption_password"
        certificate_chain = self.fetch_certificate_chain()

        if (certificate_chain[certificate_id] != "") and (certificate_chain[certificate_key_id] != "") and (certificate_chain[subscription_key_id] != ""):
            certificate_location = self.save_to_file(certificate_id,
                                                     certificate_chain[certificate_id])
            certificate_key_location = self.save_to_file(certificate_key_id,
                                                         certificate_chain[certificate_key_id])

            encryption_password = self.fetch_encryption_passphrase()
            try:
                ssl_context = ssl.SSLContext(ssl.PROTOCOL_SSLv23)
                ssl_context.load_cert_chain(certfile=certificate_location,
                                            keyfile=certificate_key_location,
                                            password=encryption_password[encryption_password_id])

                AnalyticsSubmissionEndpoint(ssl_context).submit(certificate_chain[subscription_key_id],
                                                                self.read_metrics(),
                                                                document_name)
            except ssl.SSLError as ssl_error:
                logging.error(ssl_error)
                print("SSL Error")

            self.delete_file(certificate_id)
            self.delete_file(certificate_key_id)
        else:
            print("Error, didn't send analytics. Certificate chain values not found")

    def fetch_certificate_chain(self):
        certificate_secret_name = '${certificate_secret_name}'
        key_secret_name = '${key_secret_name}'
        subscription_key_name = '${subscription_key_name}'

        try:
            # PRIVATE CERTIFICATE
            certificate_private = self.secrets_manager.get_secret_value(SecretId=certificate_secret_name)["SecretString"]
            # RSA PRIVATE KEY
            certificate_key = self.secrets_manager.get_secret_value(SecretId=key_secret_name)["SecretString"]
            subscription_key = self.secrets_manager.get_secret_value(SecretId=subscription_key_name)["SecretString"]
        except ClientError:
            print(f"Error, the certificate '{certificate_secret_name}' does not exist on this environment")
            return {"certificate_private": "", "certificate_key": "", "subscription_key": ""}
        else:
            return {"certificate_private": certificate_private,
                    "certificate_key": certificate_key,
                    "subscription_key": subscription_key}

    def fetch_encryption_passphrase(self):
        encryption_password_secret_name = '${encryption_password_secret_name}'
        try:
            # Value should be in UTF-8
            encryption_passphrase_dictionary = self.secrets_manager.get_secret_value(SecretId=encryption_password_secret_name)["SecretString"]
        except ClientError:
            print(f"Error, the secrets manager name '{encryption_password_secret_name}' does not exist on this environment")
            return {"encryption_password": ""}
        else:
            return {"encryption_password": encryption_passphrase_dictionary}

    def save_to_file(self, file_name, content):
        location = "/tmp/" + file_name
        with open(location, "w", encoding="utf8") as f:
            f.write(content)
        return location

    def delete_file(self, file_name):
        os.remove("/tmp/"+file_name)

class AnalyticsSubmissionEndpoint:

    def __init__(self, ssl_context):
        self.ssl_context = ssl_context

    def submit(self, subscription_key, metrics, metrics_name):
        host = "${aae_environment}"
        api_resource = "/c19appdata/"

        request_headers = {
            "Content-Type": "application/json",
            "Ocp-Apim-Trace": "true",
            "Ocp-Apim-Subscription-Key": subscription_key
        }
        connection = http.client.HTTPSConnection(host, port=443, context=self.ssl_context)

        endpoint_uri = api_resource + metrics_name
        connection.request(method="PUT",
                           url=endpoint_uri,
                           headers=request_headers,
                           body=json.dumps(metrics))

        response = connection.getresponse()
        if (response.status < 200) or (response.status > 299):
            print("The host did not create the record as expected.\nHTTP response is:",
                  response.status, response.reason)
        elif response.status >= 500:
            print("The host is currently unavailable.\nHTTP response is:", response.status,
                  response.reason)
        else:
            print(f"Successfully delivered metric file: {metrics_name}.\nHTTP response is:", response.status,
                  response.reason)
