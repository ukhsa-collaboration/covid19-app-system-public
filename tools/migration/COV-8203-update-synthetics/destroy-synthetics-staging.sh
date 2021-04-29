#!/usr/bin/env bash

git checkout te-staging-synth
rake synth:destroy:staging
rm -rf out/gen/synthetics
