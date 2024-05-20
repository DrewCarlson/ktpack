Kotlin Compilation Process
===

Compiling Multiplatform Kotlin projects is a multistep process which can change depending on your desired target.
This document outlines the basic process for compiling common and platform-specific sources to the various compiler outputs.

*Example Structure*

We use the following project structure which contains:

- A common `main.kt` which includes a `main()` function and `expect` declarations
- Platform specific actuals in their target common parent (i.e. `js` instead of `jsnode` and `jsbrowser`)

```
hello_world
└── src
    ├── common
    │   └── kotlin
    │       └── main.kt
    ├── jvm
    │   └── kotlin
    │       └── actuals.kt
    ├── native
    │   └── kotlin
    │       └── actuals.kt
    └── js
        └── kotlin
            └── actuals.kt
```

## JVM

The `kotlinc-jvm` compilation process is the simplest and only requires a single command to compile a JAR using common and/or platform sources.

```bash
kotlinc-jvm -d hello_world.jar \
  -Xmulti-platform \
  -Xcommon-sources=src/common/kotlin/main.kt \
  src/common/kotlin/main.kt \
  src/jvm/kotlin/actuals.kt
```

In this example, we first specify the output jar file with `-d hello_world.jar`.
Next we tell the compiler to expect multiplatform sources with `-Xmulti-platform`.
Then we list the common source files with `-Xcommon-sources=...`.
Lastly we provide the full list of platform specific sources AND the list of common source files.


## Native

```bash
kotlinc-native \
  -target macos_arm64 \
  -produce library \
  -output "out/macos_arm64/lib/common" \
  -Xmulti-platform \
  -Xcommon-sources="src/common/kotlin/main.kt" \
  "src/common/kotlin/main.kt" \
  "src/native/kotlin/actuals.kt"
```

`out/macos_arm64/common.klib`

```bash
kotlinc-native \
  -target macos_arm64 \
  -produce program \
  -output "out/macos_arm64/bin/hello_world.kexe" \
  -Xinclude="out/macos_arm64/lib/common.klib"
```


## Javascript

Similar to Native (though for different [reasons](https://youtrack.jetbrains.com/issue/KT-67089/)), Javascript targets
require a two-step compilation process for compiling binaries.

First compile the sources into a `klib` library file:

```bash
kotlinc-js \
  -Xir-produce-klib-file \
  -Xmulti-platform \
  -libraries="$KOTLIN_HOME/lib/kotlin-stdlib-js.klib" \
  -ir-output-dir "$(pwd)/out/js_node/lib" \
  -ir-output-name "common" \
  -Xcommon-sources="src/common/kotlin/main.kt" \
  src/common/kotlin/main.kt \
  src/js/kotlin/actuals.kt
```

This will produce `out/libs/lib.klib` of all the sources.
If your goal is to produce a library, the process ends here.

To produce a binary (.js) file, use the generated klib in the following command:

```bash
kotlinc-js \
  -Xir-produce-js \
  -Xmulti-platform \
  -libraries="$KOTLIN_HOME/lib/kotlin-stdlib-js.klib" \
  -ir-output-dir "$(pwd)/out/js_node/bin" \
  -ir-output-name "hello_world" \
  -Xinclude="out/js_node/lib/common.klib"
```

_Note that source files are not provided in this step._

This will create `out/bin/hello_world.js`.
