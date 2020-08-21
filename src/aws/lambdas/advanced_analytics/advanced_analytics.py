import http.client
import logging
import os
import ssl

from aws_xray_sdk.core import patch
from aws_xray_sdk.core import xray_recorder

logging.basicConfig(level=logging.INFO)

# httplib is an alias for http.client
patch((['httplib']))


@xray_recorder.capture('Advanced Analytics')
class AdvancedAnalytics:

    def __init__(self, certificate_private, certificate_key, subscription_key, encryption_passphrase, host):
        self.certificate_private = certificate_private
        self.certificate_key = certificate_key
        self.subscription_key = subscription_key
        self.encryption_passphrase = encryption_passphrase
        self.host = host

    def send(self, document_name, analytics):
        certificate_id = "certificate_private"
        certificate_key_id = "certificate_key"
        certificate_location = self.save_to_file(certificate_id, self.certificate_private)
        certificate_key_location = self.save_to_file(certificate_key_id, self.certificate_key)

        try:
            ssl_context = ssl.SSLContext(ssl.PROTOCOL_SSLv23)
            ssl_context.load_cert_chain(certfile=certificate_location,
                                        keyfile=certificate_key_location,
                                        password=self.encryption_passphrase)

            self.send_to_aa(self.subscription_key, analytics, document_name, ssl_context, self.host)

        except ssl.SSLError as ssl_error:
            logging.error(ssl_error)
            print("SSL Error")

        self.delete_file(certificate_id)
        self.delete_file(certificate_key_id)

    @staticmethod
    def save_to_file(file_name, content):
        location = "/tmp/" + file_name
        with open(location, "w", encoding="utf8") as f:
            f.write(content)
        return location

    @staticmethod
    def delete_file(file_name):
        os.remove("/tmp/" + file_name)

    @staticmethod
    def send_to_aa(key, metrics, metrics_name, ssl_context, host):
        request_headers = {
            "Content-Type": "application/json",
            "Ocp-Apim-Trace": "true",
            "Ocp-Apim-Subscription-Key": key
        }
        connection = http.client.HTTPSConnection(host, port=443, context=ssl_context)

        endpoint_uri = "/c19appdata/" + metrics_name
        connection.request(method="PUT",
                           url=endpoint_uri,
                           headers=request_headers,
                           body=metrics)

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
