# CLI Architecture

The main Ktpack CLI is a [Kotlin/Native](https://kotlinlang.org/docs/native-overview.html) program, we refer to this simply as 'cli'.
It provides most of the functionality like toolchain management, fetching resources, and launching compilers/tools.

The `ktpack-script.jar` provides the APIs necessary for `pack.kts` scripts to function.
The cli uses it to evaluate pack scripts which return a JSON result.

With the pack json model, the cli understands the project tooling and build requirements.
It can also use the `ktpack-script.jar` to invoke tasks from the pack script.

## Environment setup


### JDK

The first step when running the cli is to ensure all toolchain requirements are met.
For new installations, check for existing JDK instances and allow the user to choose one.
Alternatively a new JDK can be installed and set to the default.

### Kotlin SDK

If a JDK installation is available, we check the Kotlin version requirement and install it if missing.


