FROM ruby:2.7.1-buster AS ruby

ENV TERRAFORM_VERSION=0.12.29
ENV TERRAFORM_URL=https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip
ENV TERRAFORM_ARCHIVE=terraform_${TERRAFORM_VERSION}_linux_amd64.zip
ENV GITVERSION_VERSION=5.3.6
ENV GITVERSION_URL=https://github.com/GitTools/GitVersion/releases/download/${GITVERSION_VERSION}/gitversion-ubuntu.18.04-x64-${GITVERSION_VERSION}.tar.gz
ENV PYTHON_VERSION=3.8.3
ENV PYTHON_URL=https://www.python.org/ftp/python/${PYTHON_VERSION}/Python-${PYTHON_VERSION}.tgz
ENV AWSCLI_URL=https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip
ENV MAVEN_ULR=https://downloads.apache.org/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz

# Things needed to install the various languages. See later for additional required packages

RUN apt-get update && apt-get -qq -y install \
  autoconf \
  automake \
  build-essential \
  curl \
  dos2unix \
  git \
  graphviz \
  iputils-ping \
  jq \
  libffi-dev \
  libgdbm-dev \
  libncurses5-dev \
  libnss3-dev \
  libreadline-dev \
  libssl-dev \
  libtool \
  protobuf-compiler \
  ruby-google-protobuf \
  sudo \
  wget \
  zlib1g-dev

WORKDIR /work

# Gitversion installation
RUN wget ${GITVERSION_URL} && \
  tar -xf gitversion-ubuntu.18.04-x64-${GITVERSION_VERSION}.tar.gz && \
  cp gitversion /usr/local/bin && \
  chmod a+rx /usr/local/bin/gitversion


# Python installation
RUN wget ${PYTHON_URL} && tar -xf Python-${PYTHON_VERSION}.tgz
WORKDIR /work/Python-${PYTHON_VERSION}
RUN ./configure --enable-optimizations && \
  make -j 4 && \
  make altinstall && \
  ln -s /usr/local/bin/python3.8 /usr/local/bin/python && \
  ln -s /usr/local/bin/python3.8 /usr/local/bin/python3 && \
  ln -s /usr/local/bin/pip3.8 /usr/local/bin/pip3 && \
  ln -s /usr/local/bin/pip3.8 /usr/local/bin/pip

# AWS installation
RUN wget ${AWSCLI_URL} && \
  unzip awscli-exe-linux-x86_64.zip && \
  ./aws/install

# nodejs installation
RUN curl -sL https://deb.nodesource.com/setup_14.x | bash - && \
  apt-get install -y nodejs chromium

# Java installation
RUN apt-get update && apt-get install -y openjdk-11-jdk-headless

# Maven installation
RUN	mkdir -p /opt && cd /opt && \
  wget ${MAVEN_ULR} && \
  tar xzvf apache-maven-3.6.3-bin.tar.gz && \
  rm -f apache-maven-3.6.3-bin.tar.gz
ENV PATH="${PATH}:/opt/apache-maven-3.6.3/bin"

# Additional packages (may be installed by default, but listed here to be obvious that there is a dependency)
RUN  apt-get -qq -y install gpg groff less

# Terraform installation - later in the dockerfile, as version bumps regularly
RUN wget -q ${TERRAFORM_URL} && \
  unzip ${TERRAFORM_ARCHIVE} && \
  cp terraform /usr/local/bin && \
  chmod +x /usr/local/bin/terraform

# at the bottom as they vary more often than system level packages & languages

# Ruby gems installation
COPY Gemfile .
RUN dos2unix Gemfile
RUN bundle install

# Python packages
COPY requirements.txt .
RUN dos2unix requirements.txt
RUN pip install -r requirements.txt

COPY profile-*.sh /etc/profile.d/
COPY useradd-init /usr/local/bin

RUN dos2unix \
    /etc/profile.d/profile-*.sh \
    /usr/local/bin/useradd-init

WORKDIR /workspace

RUN rm -rf /work/*

ENTRYPOINT ["/bin/bash", "/usr/local/bin/useradd-init"]
