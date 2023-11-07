# Installation

At this time, releases are published to [Github](https://github.com/DrewCarlson/ktpack/releases).

|                                                                                                                                                                                   |                                                                             Download v{{lib_version}}                                                                             |                                                                                                                                                                                         | 
|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
| [<span style="font-size:45pt;">:fontawesome-brands-linux:</span><br/>ktpack-linux.zip](https://github.com/DrewCarlson/ktpack/releases/download/v{{lib_version}}/ktpack-linux.zip) | [<span style="font-size:45pt;">:fontawesome-brands-apple:</span><br/>ktpack-macos.zip](https://github.com/DrewCarlson/ktpack/releases/download/v{{lib_version}}/ktpack-macos.zip) | [<span style="font-size:45pt;">:fontawesome-brands-windows:</span><br/>ktpack-windows.zip](https://github.com/DrewCarlson/ktpack/releases/download/v{{lib_version}}/ktpack-windows.zip) |

## macOS

```shell
$ curl https://github.com/DrewCarlson/Ktpack/releases/latest/download/ktpack-macos.zip -o ktpack-macos.zip
$ unzip ktpack-macos.zip && cd ktpack-macos

$ ktpack version
Ktpack version 1.0.0-SNAPSHOT
```

## Windows

1. Download the latest windows release above.
2. Extract the `ktpack-windows.zip` to your system, for example `C:\Program Files\ktpack-windows\ktpack.exe`.
3. Add to your system `PATH`: Start menu, search "environment", click "Environment Variables...".

_It is recommended you use the [Terminal](https://github.com/microsoft/terminal) application for the best experience._

## Linux

```shell
$ curl https://github.com/DrewCarlson/Ktpack/releases/latest/download/ktpack-linux.zip -o ktpack-linux.zip
$ unzip ktpack-linux.zip && cd ktpack-linux

$ ktpack version
Ktpack version 1.0.0-SNAPSHOT
```

## Environment Setup

Ktpack manages all the necessary tools required to build your projects.
You can get started quickly with the `ktpack setup` command, this will check your environment for existing tools and
install any that are missing.

New machines will install a version of the JDK, Kotlin Compilers (Jvm/Native), and Nodejs.
Note that additional tool versions may be installed depending on package requirements and all versions can be
individually managed.
