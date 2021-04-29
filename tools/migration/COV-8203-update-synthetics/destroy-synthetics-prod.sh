#!/usr/bin/env bash

git checkout te-prod-synth
rake synth:destroy:prod
rm -rf out/gen/synthetics
