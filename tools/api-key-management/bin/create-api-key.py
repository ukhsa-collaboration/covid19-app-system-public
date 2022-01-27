#!/usr/bin/env python3

import argparse
import base64
import os
import random
import string
import subprocess
import sys
import zipfile
from datetime import datetime, timedelta

import bcrypt


# you can test this with: tools/api-key-management/bin/create-api-key.py --api npex --party test --environment dev
# if you import the private key for test-dev, you'll be able to verify the output.


def random_string(length):
    return "".join(random.choice(string.ascii_letters) for _ in range(length))


def log(*args):
    print(f"{datetime.now()} {os.path.basename(sys.argv[0])}", end=": ", file=sys.stderr)
    print(*args, file=sys.stderr)
    sys.stderr.flush()


def run(cmd, env=None, **kwargs):
    env = env if env is not None else {}
    try:
        return subprocess.run(cmd, env=dict(os.environ, **env),
                              check=True,
                              stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                              encoding="UTF-8", **kwargs)
    except subprocess.CalledProcessError as e:
        raise IOError(f"Failed: {cmd}\n Stdout: {e.stdout}\n StdErr: {e.stderr}")


def random_hex(digits):
    c = run(["openssl", "rand", "-hex", str(digits)])
    return c.stdout.strip()


def import_public_key(path):
    run(["gpg", "--import", path])


def gpg_key_fingerprint(keyname):
    return run(["gpg", "--fingerprint", keyname]).stdout.strip()


def generate_ephemeral_key(expiry):
    date = datetime.today().strftime("%Y-%m-%d")
    random_bits = random_string(4)
    distribution_keyname = f"tt-api-distribution-{date}-{random_bits}"
    run(["gpg", "--quick-generate-key", distribution_keyname, "rsa4096", "sign,auth,encr",
         expiry.strftime("%Y-%m-%d")])
    return distribution_keyname


def encrypt(plaintext, encryption_keyname, recipient_keyname):
    try:
        return subprocess.run(
            ["gpg",
             "--encrypt", "--cipher-algo", "AES256",
             "--sign", "--digest-algo", "SHA256",
             "--armor",
             "--local-user", encryption_keyname,
             "--recipient", recipient_keyname],
            check=True,
            encoding="UTF-8",
            input=plaintext,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE
        ).stdout.strip()
    except subprocess.CalledProcessError as e:
        raise IOError(f"Failed: gpg encrypt\n Stdout: {e.stdout}\n StdErr: {e.stderr}")


def export_public_key(keyname):
    return run(["gpg", "--export", "--armor", keyname]).stdout.strip()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Create an API key for a third party, using their public key")
    parser.add_argument("--api", required=True,
                        help="Type name of the api, e.g. `SubmitAPI` or `TestLabAPI` (validation expected externally)")
    parser.add_argument("--party", required=True, help="name of third party system, e.g. test, npex-cta, ...")
    parser.add_argument("--environment", required=True,
                        help="Deployment Environment, either dev, integration or prod (validation expected externally")
    parser.add_argument("--output", default=".", help="Output directory for the resulting zip file")
    parser.add_argument("--output-file", help="filename for the output zip file")
    parser.add_argument("--output-secret",
                        help="filename where to write the key name and bcrypt'd secret (colon delimited)")

    args = parser.parse_args()

    environment = args.environment
    party = args.party
    api = args.api

    scriptdir = os.path.realpath(os.path.dirname(__file__))
    topdir = os.path.dirname(scriptdir)
    keydir = f"{topdir}/public-keys/{environment}"

    recipient_public_key_file = f"{keydir}/{party}-{environment}-public-key.txt"

    if not os.path.exists(recipient_public_key_file):
        log(f"Public key {recipient_public_key_file} not found")
        exit(1)

    log(f"Importing public key from {recipient_public_key_file}")
    import_public_key(f"{recipient_public_key_file}")

    today = datetime.today()
    expiry = today + timedelta(days=180)
    formatted_expiry = expiry.strftime("%Y%m%d")

    keyname = f"{party}-{environment}-{formatted_expiry}"
    keyvalue = random_hex(32)
    entire_key = f"{keyname}:{keyvalue}"

    entire_key_encoded = base64.b64encode(entire_key.encode("utf-8")).decode("utf-8")

    secret_manager_location = f"/{api}/{keyname}"
    hashed_value = bcrypt.hashpw(keyvalue.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")

    if args.output_secret is not None:
        with open(args.output_secret, "w") as secret_file:
            secret_file.write(f"{keyname}:{hashed_value}")

    log("Generating an ephemeral distribution key, enter some password when prompted")
    distribution_key_name = generate_ephemeral_key(today + timedelta(days=7))

    ciphertext = encrypt(entire_key_encoded, encryption_keyname=distribution_key_name,
                         recipient_keyname=f"{party}-{environment}-key")

    zip_dir = os.path.realpath(args.output)
    zip_name = args.output_file if args.output_file is not None else f"{keyname}.zip"
    zip_path = os.path.join(zip_dir, zip_name)

    with zipfile.ZipFile(zip_path, mode="w") as z:
        with z.open(f"{keyname}.gpg.asc", mode="w") as ct:
            ct.write(ciphertext.encode("utf-8"))
        with z.open(f"{distribution_key_name}-public-key.txt", mode="w") as pk:
            pk.write(export_public_key(distribution_key_name).encode("utf-8"))

    log(f"(1) Action Required: You need to run the following in ** {environment.upper()} ** ")
    log(f"aws secretsmanager create-secret --name {secret_manager_location} --secret-string '{hashed_value}'")

    log(f"(2) Action Required: You need to email to file {zip_path} to the third party")
    log(f"and follow the trust validation procedure in tools/api-key-management: README.md")

    log(f"(3) Action Required: The key fingerprint you need to verify with them is {distribution_key_name}")
    log(f"                   : must have key fingerprint {gpg_key_fingerprint(distribution_key_name)}")
