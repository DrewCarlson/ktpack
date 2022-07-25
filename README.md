***Not ready for use!***

<p align="center">
<img src="docs/img/cover-small.png" alt="Ktpack Logo"/>
</p>

<h3 align="center">A simple tool for building and publishing Kotlin software.</h3>

<p align="center">
<img alt="MIT License" src="https://img.shields.io/github/license/drewcarlson/ktpack"/>
<img src="https://img.shields.io/github/v/release/drewcarlson/ktpack?include_prereleases" alt="GitHub release (latest SemVer including pre-releases)"/>
<img src="https://img.shields.io/github/workflow/status/drewcarlson/ktpack/Tests?label=Tests" alt="GitHub Test Workflow Status"/>
</p>

---

<p align="center">
<a href="https://drewcarlson.github.io/ktpack/"><b>Installation</b></a> -
<a href="https://drewcarlson.github.io/ktpack/"><b>Documentation</b></a> -
<a href="https://github.com/DrewCarlson/ktpack/releases/"><b>Releases</b></a>
</p>

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

| Name                              | State                  | Details                                                           |
|-----------------------------------|------------------------|-------------------------------------------------------------------|
| Publish Linux, Windows, macOS CLI | :white_check_mark:     |                                                                   |
| Build basic Jvm/native/js Modules | :white_check_mark:     |                                                                   |
| Manage K/Jvm Toolchain            | :large_blue_circle:    |                                                                   |
| Manage K/N Toolchain              | :large_blue_circle:    |                                                                   |
| Manage K/Js Toolchain             | :large_blue_circle:    |                                                                   |
| Module test sources               | :large_blue_circle:    |                                                                   |
| Compiler Plugin Support           | :large_orange_diamond: | Focus will be kx.serialization, jb-compose, and ksp support.      |
| Consume Lib Dependency            | :large_orange_diamond: |                                                                   |
| Consume Git Ktpack Libs           | :large_orange_diamond: | Add a dependency on git repos containing a ktpack lib.            |
| Consume File Ktpack Libs          | :large_orange_diamond: | Add a dependency on http/local files with embedded config.        |
| Consume Maven Dependency          | :large_orange_diamond: |                                                                   |
| Publish Maven Dependency          | :large_orange_diamond: |                                                                   |
| CInterop Dependency               | :large_orange_diamond: |                                                                   |
| IDE Support                       | :large_orange_diamond: | IntelliJ will be the first target, but VSCode will be considered. |
| Module benchmark sources          | :large_orange_diamond: |                                                                   |
| Android Support                   | :red_circle:           |                                                                   |

### License

The repository uses the MIT license, see [LICENSE](LICENSE).
