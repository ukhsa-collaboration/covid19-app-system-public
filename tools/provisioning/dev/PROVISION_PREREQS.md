# Development prerequisites

There are a limited number of tools required for installation on the developers host system to be able to work with the environments and tools provided by this project.

## Software required

* [Docker Desktop](https://www.docker.com/products/docker-desktop)
  * Before installing, please uninstall Docker Toolchain, if you have it on your machine, and remove all Docker-related environment variables.
* [AWS CLI v2](https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html)
  * After installing, create an [AWS access key](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_credentials_access-keys.html) in your IAM account and configure the AWS CLI using [aws configure](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-quickstart.html)

## Build system setup

In order to use convenience tasks for the [dockerized development environment](README.md), the [build system](../../../doc/BUILDSYSTEM.md) must be provisioned, and this depends on Ruby and a bundle of gems.

Ruby comes out of the box for [MacOs](#macos) but requires installation for [Windows](#windows).

_The number of steps to build the docker container image and the parameters to correctly start it have grown in number and complexity so that "convenience task" usage is now essential_.

Once these are installed (see relevant section below), [configure AWS credentials](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-quickstart.html), and use the convenience tasks to pull and run the [docker container](README.md) that bundles the rest of the tools and configuration.

### Windows

Run the [Ruby installer](https://github.com/oneclick/rubyinstaller2/releases/download/RubyInstaller-2.6.6-1/rubyinstaller-devkit-2.6.6-1-x64.exe) 

Then install the [gems bundle](../../build/Gemfile) as follows:

From the root of the repository

```cmd
gem i bundler
cd tools\build
bundle install
```

Depending on where you store the source code you may run into `filename too long` errors.  To remedy this, perform the steps below.
1. Run `gpedit` and navigate to *Computer Configuration* -> *Administrative Templates* -> *System* -> *Filesystem*. Mark `Enable Win32 long paths` as enabled.
2. Open command prompt as administrator and run `git config --system core.longpaths true`

### MacOS

There is a bootstrap [bash script](../bootstrap/macos.sh) to install Ruby and the required [gems bundle](../../build/Gemfile).  AWS CLI is also installed as part of this. 

A manual step is described at the end to set up your shell to use the proper version of Ruby.

The script assumes you are using ```brew``` as the MacOS package manager.

_You still have to install docker desktop and configure your AWS credentials_
