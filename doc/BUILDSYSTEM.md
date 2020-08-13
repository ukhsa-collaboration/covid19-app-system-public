# Build System Reference

This is the gaudi API documentation landing page for all helpers and tasks.

## Purpose

The purpose of the build system is to provide a consistent UI across all the different tools and technologies used to build and test the COVID-19 system and to provide a single place to define and maintain configuration parameters for the development environment.

## Assumptions

The build system is tested with and assumes an environment where all the necessary tools are installed and correctly configured - something that is handled by the development environment provisioning scripts.

## Usage and important sections

The code in the build system is namespaced in Ruby modules.

The Gaudi module contains the core of the build system which handles loading the configuration, the build system modules and provides only a limited number of helper functions and built-in tasks. These are documented in [the Gaudi repository](https://github.com/damphyr/gaudi).

If an entry has no easily recognized module prefix (Gaudi:: or NHSX::) then it is a safe bet that it is functionality that comes from a public library/gem.

### Environment variables

By convention gaudi uses environment variables to pass parameters to its tasks and control functionality. This is described in more detail in the [core documentation](https://github.com/damphyr/gaudi/blob/master/doc/CONFIGURATION.md).

Environment variables that influence/parametrize the build system behaviour are documented in Gaudi::Configuration::EnvironmentOptions.

### Generate the reference documentation

To generate the documentation under `*out/doc/gaudi*`, run

```bash
rake doc:gaudi
```

If your development environment includes graphviz then you can generate a graphical overview of the tasks and their dependencies by running

```bash
rake doc:graph:gaudi
```
