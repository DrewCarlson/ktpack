# CLI Architecture

The main Ktpack CLI is a [Kotlin/Native](https://kotlinlang.org/docs/native-overview.html) program.
It provides most of the functionality like toolchain management, fetching resources, and launching compilers/tools.

The `ktpack-script.jar` provides the APIs necessary for `pack.kts` scripts to function.
The cli uses it to evaluate pack scripts which return a JSON result.

With the pack json model, the cli understands the project tooling and build requirements.
It can also use the `ktpack-script.jar` to invoke tasks from the pack script.
