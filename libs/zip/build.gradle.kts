plugins {
    `cpp-library`
}

library {
    baseName.set("zip")
    linkage.set(listOf(Linkage.STATIC))
    targetMachines.addAll(
        listOfNotNull(
            machines.linux.x86_64,
            machines.windows.x86_64,
            machines.macOS.x86_64,
        )
    )
    binaries.configureEach {
        compileTask.get().apply {
            includes(rootProject.file("external/zip/src"))
            source(rootProject.file("external/zip/src/zip.c"))
            compilerArgs.addAll("-x", "c")
        }
    }
}
