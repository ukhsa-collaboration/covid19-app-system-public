FROM ruby:3.1-slim-bullseye AS ruby

ENV TERRAFORM_VERSION=1.1.9
ENV TERRAFORM_URL=https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip
ENV TERRAFORM_ARCHIVE=terraform_${TERRAFORM_VERSION}_linux_amd64.zip
ENV GITVERSION_VERSION=5.8.1
ENV GITVERSION_URL=https://github.com/GitTools/GitVersion/releases/download/${GITVERSION_VERSION}/gitversion-linux-x64-${GITVERSION_VERSION}.tar.gz
ENV PANDOC_VERSION=2.11.3.2
ENV PANDOC_URL=https://github.com/jgm/pandoc/releases/download/${PANDOC_VERSION}/pandoc-${PANDOC_VERSION}-linux-amd64.tar.gz
ENV PANDOC_CROSSREF_VERSION=v0.3.9.0a
ENV PANDOC_CROSSREF_URL=https://github.com/lierdakil/pandoc-crossref/releases/download/${PANDOC_CROSSREF_VERSION}/pandoc-crossref-Linux.tar.xz
ENV AWSCLI_URL=https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip
ENV NODE_VERSION=14

# Things needed to install the various languages. See later for additional required packages
RUN echo 'deb http://http.us.debian.org/debian/ testing non-free contrib main' >> /etc/apt/sources.list && \
  apt-get update && apt-get -qq -y install \
  autoconf \
  automake \
  apt-transport-https \
  bash-completion \
  build-essential \
  curl \
  dos2unix \
  git \
  gnuplot \
  graphviz \
  gpg \
  groff \
  iputils-ping \
  jq \
  less \
  libffi-dev \
  libgdbm-dev \
  libncurses5-dev \
  libnss3-dev \
  libreadline-dev \
  libssl-dev \
  libtool \
  nano \
  openjdk-11-jdk-headless \
  protobuf-compiler \
  python3 \
  python3-pip \
  python3-venv \
  ruby-google-protobuf \
  sudo \
  texlive-latex-base \
  texlive-fonts-recommended \
  texlive-fonts-extra \
  texlive-latex-extra \
  texlive-xetex \
  wget \
  zlib1g-dev && \
  apt autoremove -y && \
  ln -s /usr/bin/python3 /usr/bin/python

WORKDIR /work

# Gitversion installation
RUN wget ${GITVERSION_URL} && \
  tar -xf gitversion-*-x64-${GITVERSION_VERSION}.tar.gz && \
  cp gitversion /usr/local/bin && \
  chmod a+rx /usr/local/bin/gitversion

RUN wget ${PANDOC_URL} && \
  tar -xf pandoc-${PANDOC_VERSION}-linux-amd64.tar.gz && \
  cp pandoc-${PANDOC_VERSION}/bin/pandoc /usr/local/bin && \
  chmod a+rx /usr/local/bin/pandoc

RUN wget ${PANDOC_CROSSREF_URL} && \
  tar -xf pandoc-crossref-Linux.tar.xz && \
  cp pandoc-crossref /usr/local/bin && \
  chmod a+rx /usr/local/bin/pandoc-crossref

# AWS installation
RUN wget ${AWSCLI_URL} && \
  unzip awscli-exe-linux-x86_64.zip && \
  ./aws/install && \
  curl \
  "https://s3.amazonaws.com/session-manager-downloads/plugin/latest/ubuntu_64bit/session-manager-plugin.deb" \
  -o "session-manager-plugin.deb" && \
  dpkg -i session-manager-plugin.deb

# nodejs installation -- version must match definition of "runtime" in any "aws_lambda_function" resource
RUN curl -fsSL https://deb.nodesource.com/setup_${NODE_VERSION}.x | bash - && \
  apt-cache policy nodejs && \
  apt-get install -y nodejs npm chromium

# yarn v2 and gulp installation
RUN npm install --global corepack && \
  npm i -g gulp

# Terraform installation - later in the dockerfile, as version bumps regularly
RUN wget -q ${TERRAFORM_URL} && \
  unzip ${TERRAFORM_ARCHIVE} && \
  cp terraform /usr/local/bin && \
  chmod +x /usr/local/bin/terraform

# at the bottom as they vary more often than system level packages & languages

# Ruby gems installation
COPY Gemfile .
RUN dos2unix Gemfile && \
  bundle install

# Python packages
COPY requirements.txt .
RUN dos2unix requirements.txt && \
  pip install -r requirements.txt

COPY profile-*.sh /etc/profile.d/
COPY useradd-init /usr/local/bin

RUN dos2unix \
    /etc/profile.d/profile-*.sh \
    /usr/local/bin/useradd-init

WORKDIR /workspace

RUN rm -rf /work/*

ENTRYPOINT ["/bin/bash", "/usr/local/bin/useradd-init"]
