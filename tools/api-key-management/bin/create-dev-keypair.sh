#!/bin/bash

set -e

MYDIR=$(dirname $0)

SYSTEM=test
ENVIRONMENT=dev

KEYDIR=${MYDIR}/../public-keys

STEM=${KEYDIR}/${ENVIRONMENT}/${SYSTEM}-${ENVIRONMENT}

gpg --quick-generate-key "${SYSTEM}-${ENVIRONMENT}-key" rsa4096 sign,auth,encr 2022-01-01

gpg --export --armor "${SYSTEM}-${ENVIRONMENT}-key" > ${STEM}-public-key.txt

