# assume we use the system Ruby and brew is available
if docker --version;
then
  brew install awscli
  cd ../../build
  sudo gem i bundler
  bundle install >/dev/null
  echo "***********************************************************************************************************"
  echo "* Configure your environment using 'aws configure' and then run 'rake provision:devenv:pull' and then 'rake devenv' in the root of the repository"
else
  echo "Install docker desktop first"
fi
