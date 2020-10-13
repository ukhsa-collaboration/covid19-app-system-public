# Makefile that doesn't run the tests on a 'sam build'
# because otherwise the tests run all the time?? - not very useful if you are experimenting locally

makefile_dir := $(dir $(lastword $(MAKEFILE_LIST)))

TOPDIR=$(realpath $(makefile_dir)/../../..)
SRCS=$(shell find $(TOPDIR)/src)

POM=$(TOPDIR)/pom.xml

$(TOPDIR)/target/javalambda-0.0.1-SNAPSHOT.jar: $(POM) $(SRCS)
	mvn -Dmaven.test.skip=true -f $(POM) package

build-Function: $(TOPDIR)/target/javalambda-0.0.1-SNAPSHOT.jar
	(cd $(ARTIFACTS_DIR) && jar xf $(TOPDIR)/target/javalambda-0.0.1-SNAPSHOT.jar)
