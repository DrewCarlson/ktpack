This page provides a brief intro to the `ktpack` command line tool.
We will create a new project, build the source code, and run the compiled program.

Create a new project with `new` command:

```shell
$ ktpack new hello_world
```

By default, the `--bin` flag is used to create a binary program.
Alternatively the `--lib` flag can be used to create a library.

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
version = "0.0.1"
kotlin-version = "1.7.10"
targets = [ "common_only" ]
```

The [Manifest]() contains all the metadata required to operate a Ktpack project.

`src/main.kt` contains our simple hello world program:

```kotlin
fun main() {
    println("Hello, world!")
}
```

Cargo generated this program which can be compiled into a runnable binary:

```shell
$ ktpack build

Compiling hello_world v1.0.0 (/users/developer/hello_world)
```

To run the executable we compiled:

```shell
$ ./out/linux_x64/debug/bin/hello_world.kexe
Hello, world!
```

Alternatively you can use the `ktpack run` command:

```shell
$ ktpack run
Compiling hello_world v1.0.0 (/users/developer/hello_world)
Running '/users/developer/hello_world/hello_world.exe'
Hello, World!
```