This page provides a brief intro to the `ktpack` command line tool.
We will create a new project, build the source code, and run the compiled program.

Create a new project with the `new` command:

```shell
$ ktpack new hello_world
```

By default, the `--bin` flag is used to create a binary program.
Alternatively the `--lib` flag can be used to create a library.
The command generated two files:

```
$ cd hello_world
$ tree .
.
├── manifest.toml
└── src
    └── main.kt

1 directory, 2 files
```

This is a basic, yet fully functional Ktpack project.
The project is described in `manifest.toml`:

```toml
[module]
name = "hello_world"
version = "1.0.0"
kotlin-version = "1.7.10"
targets = [ "common_only" ]
```

The [Manifest]() contains all the metadata required to operate a Ktpack project.
In `src/main.kt` we have this program:

```kotlin
fun main() {
    println("Hello, world!")
}
```

The source file can be compiled into a binary with the `ktpack build` command:

```shell
$ ktpack build

Compiling hello_world v1.0.0 (/users/developer/hello_world)
```

To run the executable we compiled:

```shell
$ ./out/linux_x64/debug/bin/hello_world.kexe
Hello, world!
```
_Note: the `linux_x64` directory could also be `macosx_[x64|arm64]` or `windows_x64` based on your operating system_

Alternatively we can use the `ktpack run` command:

```shell
$ ktpack run

Compiling hello_world v1.0.0 (/users/developer/hello_world)
Running 'out/linux_x64/debug/bin/hello_world.exe'
Hello, World!
```

To build or run our program for a different target, use the `--target` or `-t` option with the `build` or `run` commands:

```shell
$ ktpack run --target jvm

Compiling hello_world v1.0.0 (/users/developer/hello_world)
Running 'out/jvm/debug/bin/hello_world.jar'
Hello, World!
```

```shell
$ ktpack run --target js_node

Compiling hello_world v1.0.0 (/users/developer/hello_world)
Running 'out/js_node/debug/bin/hello_world.js'
Hello, World!
```
