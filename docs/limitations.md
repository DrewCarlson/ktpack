# Limitations

## No direct IDE support

[https://github.com/DrewCarlson/ktpack/issues/85](https://github.com/DrewCarlson/ktpack/issues/85)

There is no plugin available for Intellij to understand a Ktpack project.
You can still open a Ktpack project in Intellij IDEA and manually specify source folders/dependencies.

In the future, this will be implemented either by an IDE plugin or a CLI command to sync Intellij IDEA project files.

## No incremental compilation support

[https://github.com/DrewCarlson/ktpack/issues/86](https://github.com/DrewCarlson/ktpack/issues/86)

This would require using the Kotlin Compiler daemon which is not currently supported.
For every build or test run, you are paying the cost of initializing the JVM and Kotlin Compiler.

## JVM testing only supports JUnit 5 Jupiter

[https://github.com/DrewCarlson/ktpack/issues/82](https://github.com/DrewCarlson/ktpack/issues/82)

The JUnit 4 pom file contains formatting that Ktpack does not currently deal with, so it cannot yet be added as a
dependency.
To keep things simple, ktpack will always run JVM tests with the standalone console jar.

## No Javascript test support

[https://github.com/DrewCarlson/ktpack/issues/83](https://github.com/DrewCarlson/ktpack/issues/83)

Only JVM and Native targets can be tested at the moment.
Running Javascript tests requires more infrastructure and tooling (especially for browser) that is not currently
implemented.

## Native tests do not produce report files

[https://github.com/DrewCarlson/ktpack/issues/84](https://github.com/DrewCarlson/ktpack/issues/84)

I assume the Kotlin Gradle plugin achieves this by parsing the test run stdout and creating a JUnit results file.
Until that is implemented, Native tests will only provide feedback in the form of console output.
