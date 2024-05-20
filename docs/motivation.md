# Motivation

A basic Kotlin library/application with Gradle requires an excessive number of
files and configuration script.

This is a poor experience for regular Kotlin users and especially first time users.
Additionally, users who frequently author small or medium-sized libraries must repeat
and maintain this boilerplate across projects.

Ktpack focuses on a finite set of features from a rigid but thorough project structure,
allowing you to get more out of your code with less script configuration to glue things together.
Ktpack follows [Cargo](https://doc.rust-lang.org/cargo/index.html) closely for inspiration.


## Amper

JetBrains [Amper](https://github.com/JetBrains/amper?tab=readme-ov-file#amper) is an experimental first part build tool.
This is certainly an interesting project, especially with the addition of standalone configuration.
Overall the way Amper is described by JetBrains, it shares many of the same goals as Ktpack.
That said it is still primarily a Gradle overlay that simplifies KMP project configuration.

Amper has a number of advantages over Ktpack.
JetBrains has team of people contributing to Amper, so they're moving much faster and with a higher quality than Ktpack.
A major example of this is IDE support: Amper has some level of support in both IDEA and Fleet while Ktpack has none at this point.

I'm excited to see where Amper goes, but I still find value in building Ktpack.
Ktpack aims to remove Gradle from the equation altogether, being an overlay is not sufficient.
It also focuses more on common KMP library and native application projects, where Amper has more effort going into Android/iOS support.

I look forward to seeing where Amper ends up, but I'm unlikely to stop building Ktpack anytime soon (however slow my progress is).
