# Installation

Releases are available from [Github](https://github.com/DrewCarlson/ktpack/releases) and [Homebrew](https://brew.sh/).

|                                                                                                                                                                                   |                                                                             Download v{{lib_version}}                                                                             |                                                                                                                                                                                         | 
|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
| [<span style="font-size:45pt;">:fontawesome-brands-linux:</span><br/>ktpack-linux.zip](https://github.com/DrewCarlson/ktpack/releases/download/v{{lib_version}}/ktpack-linux.zip) | [<span style="font-size:45pt;">:fontawesome-brands-apple:</span><br/>ktpack-macos.zip](https://github.com/DrewCarlson/ktpack/releases/download/v{{lib_version}}/ktpack-macos.zip) | [<span style="font-size:45pt;">:fontawesome-brands-windows:</span><br/>ktpack-windows.zip](https://github.com/DrewCarlson/ktpack/releases/download/v{{lib_version}}/ktpack-windows.zip) |

## macOS

### Homebrew

```bash
brew install drewcarlson/repo/ktpack
```

### Manual

```shell
curl https://github.com/DrewCarlson/Ktpack/releases/download/v{{lib_version}}/ktpack-macos.zip -o ktpack-macos.zip
unzip ktpack-macos.zip && cd ktpack-macos
```

## Windows

1. Download the Windows release above.
2. Extract the `ktpack-windows.zip` to your system, for example `C:\Program Files\ktpack-windows\ktpack.exe`.
3. Add to your system `PATH`: Start menu, search "environment", click "Environment Variables...".

_It is recommended you use the [Terminal](https://github.com/microsoft/terminal) application for the best experience._

## Linux

```shell
curl https://github.com/DrewCarlson/Ktpack/releases/download/v{{lib_version}}/ktpack-linux.zip -o ktpack-linux.zip
unzip ktpack-linux.zip && cd ktpack-linux
```

## Next Steps

Run `ktpack setup`

This will find and install the various toolchains required to build and run your Kotlin projects. 
