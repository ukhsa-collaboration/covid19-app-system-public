#!/bin/bash

set -e

basepath=$(readlink -f $(dirname $0)/../..)

pom=$basepath/src/aws/lambdas/incremental_distribution/pom.xml
output=$basepath/out/java/batch_creation

mvn -f${pom} -DbuildOutput=${output} compile exec:java -Dexec.mainClass="uk.nhs.nhsx.activationsubmission.persist.InsertActivationCodesMain" \
  -Dexec.args="$*"
