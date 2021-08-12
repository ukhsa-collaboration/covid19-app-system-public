# Build System

> Note: The build system is based on [Rake](https://github.com/ruby/rake/blob/master/README.rdoc), using [Gaudi](https://github.com/damphyr/gaudi/blob/master/README.md) to organise tasks and helpers.

## Purpose

The purpose of the build system is to provide a consistent UI across all the different tools and technologies used to build and test the COVID-19 App System and to provide a single place to define and maintain configuration parameters for the development environment.

## Assumptions

The build system is tested with and assumes an environment where all the necessary tools are installed and correctly configured - something that is handled by the development environment provisioning scripts.

## Usage and important sections

The code in the build system is namespaced in Ruby modules.

The Gaudi module contains the core of the build system which handles loading the configuration, the build system modules and provides only a limited number of helper functions and built-in tasks. These are documented in the [Gaudi](https://github.com/damphyr/gaudi) repository.

If an entry has no easily recognized module prefix (Gaudi:: or NHSX::) then it is a safe bet that it is functionality that comes from a public library/gem.

### Environment variables

By convention, Gaudi uses environment variables to pass parameters to its tasks and control functionality. This is described in more detail in the [Gaudi core documentation](https://github.com/damphyr/gaudi/blob/master/doc/CONFIGURATION.md).

Environment variables that influence/parameterise the build system behaviour are defined in [Gaudi::Configuration::EnvironmentOptions](../tools/build/lib/gaudi/helpers/environment.rb).

### System Configuration
The top-level [Rakefile](../Rakefile) sets up Gaudi, which then loads the [system configuration](../tools/build/system.cfg) and modules.

### Find out what tasks are available
From the [root](..) of the repository

List all tasks, with comments
```
rake -T
```
List all tasks, with comments, that match a regular expression pattern
```
rake -T <pattern>
```

### Generate the reference documentation

Generate the documentation under `out/doc/gaudi`

```bash
rake doc:gaudi
```

Generate a graphical overview (graphviz) of the tasks and their dependencies

```bash
rake doc:graph:gaudi
```
