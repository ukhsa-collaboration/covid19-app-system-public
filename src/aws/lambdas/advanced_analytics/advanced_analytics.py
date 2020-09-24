import logging
import ssl
import tempfile

import requests
from aws_xray_sdk.core import xray_recorder
from requests.adapters import HTTPAdapter

log = logging.getLogger(__name__)


class SecureClientAuthAdapter(HTTPAdapter):

    def __init__(self, sslcontext):
        self._sslcontext = sslcontext
        super().__init__()

    def init_poolmanager(self, *args, **kwargs):
        kwargs['ssl_context'] = self._sslcontext
        return super(SecureClientAuthAdapter, self).init_poolmanager(*args, **kwargs)


@xray_recorder.capture('Advanced Analytics')
class AdvancedAnalytics:

    def __init__(self, host, secrets):
        self.session = requests.Session()
        self.session.mount(f"https://{host}", SecureClientAuthAdapter(AdvancedAnalytics.configure_ssl_context(secrets)))
        self.session.headers.update({
            "Content-Type": "application/octet-stream",
            "Ocp-Apim-Trace": "true",
            "Ocp-Apim-Subscription-Key": secrets.subscription_key
        })
        self.host = host

    @staticmethod
    def configure_ssl_context(secrets):
        with tempfile.TemporaryDirectory() as temp:
            certificate_location = AdvancedAnalytics.save_to_file(temp, secrets.certificate_private)
            certificate_key_location = AdvancedAnalytics.save_to_file(temp, secrets.certificate_key)

            ssl_context = ssl.SSLContext(ssl.PROTOCOL_TLSv1_2)
            ssl_context.load_cert_chain(certfile=certificate_location,
                                        keyfile=certificate_key_location,
                                        password=secrets.encryption_passphrase)
            return ssl_context

    @staticmethod
    def save_to_file(directory, content):
        file = tempfile.NamedTemporaryFile(dir=directory, mode="w", delete=False)
        with file as f:
            f.write(content)
        return file.name

    def send(self, document_name, analytics):

        try:
            response = self.session.put(
                f"https://{self.host}/c19appdata/{document_name}",
                data=analytics,
                timeout=(15, 15)  # connect , read
            )
            response.raise_for_status()
            log.info(f"Successfully delivered metric file: {document_name}")
            log.info(f"HTTP response is: {response.status_code}, {response.reason}")

        except requests.exceptions.HTTPError as unsuccessful:
            response = unsuccessful.response
            status = response.status_code
            if (status < 200) or (status > 299 and status < 500):
                log.error('The host did not create the record as expected.')
            elif status >= 500:
                log.error('The host is currently unavailable')

            log.error(f"HTTP response is: {status}, {response.reason}")
            raise
        except requests.ConnectionError as connection_error:
            log.error(f"Sending to AAE failed with Connection Error: {connection_error}")
            raise
