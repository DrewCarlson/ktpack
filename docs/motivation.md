# Motivation

A basic Kotlin library/application with Gradle requires an excessive number of
files and configuration script.

This is a poor experience for regular Kotlin users and especially first time users.
Additionally, users who frequently author small or medium-sized libraries must repeat
and maintain this boilerplate across projects.

Ktpack focuses on a finite set of features from a rigid but thorough project structure,
allowing you to get more out of your code with less script configuration to glue things together.
Ktpack follows [Cargo](https://doc.rust-lang.org/cargo/index.html) closely for inspiration.
