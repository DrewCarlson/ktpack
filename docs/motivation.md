# Motivation

Kotlin projects using gradle that support publishing, binary/library outputs,
multiplatform support, tests, etc. require 7+ files with various scripts,
configuration, and a specific folder structure.

This is a poor experience for regular Kotlin users and especially first time users.
Additionally, users who frequently author small or medium-sized libraries must repeat
and maintain this boilerplate across projects.

Ktpack focuses on a finite set of features from a rigid but thorough project structure,
allowing you to get more out of your code with less script configuration to glue things together.
Ktpack follows [Cargo](https://doc.rust-lang.org/cargo/index.html) closely for inspiration.
