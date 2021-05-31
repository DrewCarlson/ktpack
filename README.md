***Not ready for use!***

Ktpack
===
![GitHub Workflow Status](https://img.shields.io/github/workflow/status/drewcarlson/ktpack/Tests?label=Tests)

Build, package, and distribute Kotlin software with ease.

### About

Ktpack offers a simple path to creating multiplatform Kotlin packages, based heavily on [Cargo](https://doc.rust-lang.org/cargo/index.html).

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

_(ensure submodules are initialized and updated!! If unsure, run `git submodule update --init`)_

For Linux and macOS, there should be no extra steps.

#### Windows

- Install [msys2](https://www.msys2.org/)
- Add `C:\msys64\mingw64\bin` to the top of your `PATH` variable (restart intellij)
- Open `MSYS2 MinGW 64-bit` and run `mv /mingw64/bin/gcc /mingw64/bin/gcc-disable`
- Run the `ktpack[windowsX64]` target from Intellij

Compiling native components with Gradle is generally unpleasant and difficult to control.
Gradle does not allow manual selection of the C/C++ compiler and defaults to Visual Studio or GCC if available.
This results in library outputs that Kotlin/Native's toolchain cannot process, therefore we must disable GCC/Visual Studio so Clang is selected.
