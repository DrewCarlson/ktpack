***Not Ready for use!***
-
Ktpack is built with Kotlin/Native, requiring more low level code than your average project.
Interacting with the filesystem and starting processes is complex and OS dependent.
Current efforts focus on providing the various low level tools across Linux, Windows, and macOS.

To be clear: There is nothing to install/use at this time, but general suggestions and contributions to the previously described tasks are welcome.

Ktpack
===

Build, package, and distribute Kotlin software with ease.

### About

Ktpack offers a simple path to creating multiplatform Kotlin packages, based heavily on [Cargo](https://doc.rust-lang.org/cargo/index.html).

As an alternative to Gradle's unparalleled complexity, Ktpack provides a simple module system to create fully featured libraries and binaries in just two files.

### Installation

#### Linux

#### macOS

#### Windows

### Usage

```
Usage: ktpack [OPTIONS] COMMAND [ARGS]...

  Build, package, and distribute Kotlin software with ease.

Options:
  -h, --help  Show this message and exit

Commands:
  check    Check a package for errors.
  build    Compile packages and dependencies.
  run      Compile and run binary packages.
  test     Compile and run test suites.
  new      Create a new package.
  init     Create a new package in an existing directory.
  clean    Remove generated artifacts and folders.
  version  Show Ktpack version information.
```

### Development

Checkout with `git clone git@github.com:DrewCarlson/ktpack.git --recurse-submodules`
