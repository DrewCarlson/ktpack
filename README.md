***Not ready for use!***

Ktpack
===
![GitHub release (latest SemVer including pre-releases)](https://img.shields.io/github/v/release/drewcarlson/ktpack?include_prereleases)
![GitHub Workflow Status](https://img.shields.io/github/workflow/status/drewcarlson/ktpack/Tests?label=Tests)

The simplest way to build and publish Kotlin software.

---
**[Installation](https://drewcarlson.github.io/ktpack/)** -
**[Documentation](https://drewcarlson.github.io/ktpack/)**
---

### Overview

Ktpack is a build tool for [Kotlin Multiplatform](https://kotl.in/multiplatform) projects
providing a comprehensive set of features with one configuration file:

- Multiple binary application outputs and library modules
- Write tests and benchmarks inside or next to main source files
- Automatic Kotlin, JVM, and JS toolchain management
- Consume Maven dependencies (with custom repositories) or npm/yarn dependencies
- Publishing Gradle/Maven compatible libraries to Maven Central or other repositories

### Feature Status

| Status             | Icon                   |
|--------------------|------------------------|
| Implemented        | :white_check_mark:     |
| In Progress        | :large_blue_circle:    |
| Planned            | :large_orange_diamond: |
| Will Not Implement | :red_circle:           |

| Name                     | State                   |
|--------------------------|-------------------------|
| Build Jvm Modules        | :large_blue_circle:     |
| Build K/N Modules        | :large_blue_circle:     |
| Build K/Js Modules       | :large_blue_circle:     |
| Build Mpp Modules        | :large_blue_circle:     |
| Manage K/Jvm Toolchain   | :large_blue_circle:     |
| Manage K/N Toolchain     | :large_blue_circle:     |
| Manage K/Js Toolchain    | :large_blue_circle:     |
| Module test sources      | :large_blue_circle:     |
| Module benchmark sources | :large_orange_diamond:  |
| Consume Lib Dependency   | :large_orange_diamond:  |
| Consume Maven Dependency | :large_orange_diamond:  |
| Publish Maven Dependency | :large_orange_diamond:  |
| CInterop Dependency      | :large_orange_diamond:  |
| IDE Support              | :large_orange_diamond:  |
| Android Support          | :red_circle:            |

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

### License

The repository uses the MIT license, see [LICENSE](LICENSE).
