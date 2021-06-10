# Makefile that doesn't run the tests on a 'sam build'
# because otherwise the tests run all the time?? - not very useful if you are experimenting locally

makefile_dir := $(dir $(lastword $(MAKEFILE_LIST)))

TOPDIR=$(realpath $(makefile_dir)/../../..)
SRCS=$(shell find $(TOPDIR)/src)

POM=$(TOPDIR)/build.gradle

ZIPFILE=$(TOPDIR)/cta/build/distributions/cta-0.0.1-SNAPSHOT.zip

$(ZIPFILE): $(POM) $(SRCS)
	(cd $(TOPDIR) && $(TOPDIR)/gradlew zipLambdaDistribution)

build-Function: $(ZIPFILE)
	(cd $(ARTIFACTS_DIR) && jar xf $(ZIPFILE))
