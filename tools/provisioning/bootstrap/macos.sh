#!/usr/bin/env bash

# assume brew is available
if docker --version;
then
  brew install awscli
  brew install chruby
  brew install ruby-install
  ruby-install ruby 2.7.2
  chruby ruby-2.7.2
  cd ../../build
  gem i bundler
  bundle install >/dev/null
  echo "***********************************************************************************************************"
  echo "Add the following two lines to your ~/.profile or ~/.bashrc"
  echo " source /usr/local/opt/chruby/share/chruby/chruby.sh"
  echo " chruby ruby-2.7.2"
  echo "***********************************************************************************************************"
  echo "* Configure your environment using 'aws configure' and then run 'rake provision:devenv:pull' and then 'rake devenv' in the root of the repository"
else
  echo "Install docker desktop first"
fi
